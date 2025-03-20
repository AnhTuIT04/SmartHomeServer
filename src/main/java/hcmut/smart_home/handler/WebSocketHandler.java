package hcmut.smart_home.handler;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.cloudinary.json.JSONException;
import org.cloudinary.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import hcmut.smart_home.dto.sensor.SensorData;
import hcmut.smart_home.util.Jwt;
import hcmut.smart_home.util.Pair;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private final Jwt jwt;
    private final Firestore firestore;
    private final FirebaseDatabase firebaseDatabase;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    private final ConcurrentHashMap<WebSocketSession, Pair<String, String>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocketSession, ValueEventListener> sensorListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocketSession, ValueEventListener> controlListeners = new ConcurrentHashMap<>();

    public WebSocketHandler(Jwt jwt, Firestore firestore, FirebaseDatabase firebaseDatabase) {
        this.jwt = jwt;
        this.firestore = firestore;
        this.firebaseDatabase = firebaseDatabase;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null || uri.getQuery() == null || !uri.getQuery().startsWith("token=")) {
                session.sendMessage(new TextMessage("Error: Unauthorized"));
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String token = uri.getQuery().split("token=")[1];
            if (token == null || !jwt.validateAccessToken(token)) {
                session.sendMessage(new TextMessage("Error: Unauthorized"));
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String userId = jwt.extractId(token);
            DocumentSnapshot snapshot = firestore.collection("users").document(userId).get().get();
            if (!snapshot.exists()) {
                session.sendMessage(new TextMessage("Error: User not found"));
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String sensorId = snapshot.getString("sensorId");
            if (sensorId == null) {
                session.sendMessage(new TextMessage("Error: Sensor not found"));
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            sessions.put(session, new Pair<>(userId, sensorId));

            DatabaseReference sensorRef = firebaseDatabase.getReference(sensorId);
            DatabaseReference controlRef = firebaseDatabase.getReference(sensorId + "_control");
            SensorData data = new SensorData();

            ValueEventListener sensorListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        data.updateData(snapshot.getValue());
                        if (!data.isSendable()) {
                            data.setSendable();
                        } else {
                            sendDataToClient(session, data);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    handleDatabaseError(session, error);
                }
            };
            sensorRef.addValueEventListener(sensorListener);
            sensorListeners.put(session, sensorListener);

            ValueEventListener controlListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        data.updateData(snapshot.getValue());
                        if (!data.isSendable()) {
                            data.setSendable();
                        } else {
                            sendDataToClient(session, data);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    handleDatabaseError(session, error);
                }
            };
            controlRef.addValueEventListener(controlListener);
            controlListeners.put(session, controlListener);

        } catch (IOException | ExecutionException | InterruptedException e) {
            logger.error("Error while establishing connection: " + e.getMessage());
        }
    }

    private void sendDataToClient(WebSocketSession session, SensorData data) {
        try {
            session.sendMessage(new TextMessage(data.toString()));
        } catch (IOException e) {
            logger.error("Error while sending data to client: " + e.getMessage());
        }
    }

    private void handleDatabaseError(WebSocketSession session, DatabaseError error) {
        try {
            session.sendMessage(new TextMessage("Error: " + error.getMessage()));
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            logger.error("Error while closing session: " + e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();

            // Parse JSON payload
            JSONObject jsonMessage = new JSONObject(payload);
            String action = jsonMessage.optString("action");
            String device = jsonMessage.optString("device");
            String state = jsonMessage.optString("state");

            // Check if the message is a control message
            if ("toggle".equals(action) && !device.isEmpty() && !state.isEmpty()) {
                String sensorId = sessions.get(session).getSecond();
                if (sensorId != null) {
                    DatabaseReference controlRef = firebaseDatabase.getReference(sensorId + "_control");
                    if ("fan".equals(device)) {
                        controlRef.child("button_for_fan").setValueAsync("on".equals(state) ? 1 : 0);
                    } else if ("light".equals(device)) {
                        controlRef.child("button_for_led").setValueAsync("on".equals(state) ? 1 : 0);
                    }
                } else {
                    session.sendMessage(new TextMessage("Error: Sensor not found for user."));
                }
            }
        } catch (IOException | JSONException e) {
            logger.error("Error while handling message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        DatabaseReference sensorRef = firebaseDatabase.getReference(sessions.get(session).getSecond());
        sensorRef.removeEventListener(sensorListeners.remove(session));
    
        DatabaseReference controlRef = firebaseDatabase.getReference(sessions.get(session).getSecond() + "_control");
        controlRef.removeEventListener(controlListeners.remove(session));

        sessions.remove(session);
    }
} 
