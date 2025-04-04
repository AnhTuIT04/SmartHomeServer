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

import com.google.api.core.ApiFuture;
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
public class WebSocketRealtimeHandler extends TextWebSocketHandler {
    private final Jwt jwt;
    private final Firestore firestore;
    private final FirebaseDatabase firebaseDatabase;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketRealtimeHandler.class);

    private final ConcurrentHashMap<WebSocketSession, Pair<String, String>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocketSession, ValueEventListener> sensorListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocketSession, ValueEventListener> controlListeners = new ConcurrentHashMap<>();

    public WebSocketRealtimeHandler(Jwt jwt, Firestore firestore, FirebaseDatabase firebaseDatabase) {
        this.jwt = jwt;
        this.firestore = firestore;
        this.firebaseDatabase = firebaseDatabase;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null || uri.getQuery() == null || !uri.getQuery().startsWith("token=")) {
                session.sendMessage(new TextMessage("{\"error\": \"Unauthorized\"}"));
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String token = uri.getQuery().split("token=")[1];
            if (token == null || !jwt.validateAccessToken(token)) {
                session.sendMessage(new TextMessage("{\"error\": \"Unauthorized\"}"));
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String userId = jwt.extractId(token);
            DocumentSnapshot snapshot = getSnapshotSafely(firestore.collection("users").document(userId).get());
            if (!snapshot.exists()) {
                session.sendMessage(new TextMessage("{\"error\": \"User not found\"}"));
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String sensorId = snapshot.getString("sensorId");
            if (sensorId == null) {
                session.sendMessage(new TextMessage("{\"error\": \"Sensor not found\"}"));
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

        } catch (IOException | IllegalArgumentException | NullPointerException e) {
            logger.error("Error while establishing connection: ", e);
            try {
                session.sendMessage(new TextMessage("{\"error\": \"Internal server error\"}"));
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException ioException) {
                logger.error("Error closing session: ", ioException);
            }
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
            session.sendMessage(new TextMessage("{\"error\": " + error.getMessage() + "}"));
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
            String ledMode = jsonMessage.optString("led_mode");
            String brightness = jsonMessage.optString("led_brightness");
            String fanMode = jsonMessage.optString("fan_mode");

            // Update real-time database
            String sensorId = sessions.get(session).getSecond();
            if (sensorId != null) {
                DatabaseReference controlRef = firebaseDatabase.getReference(sensorId + "_control");
                if (!ledMode.isEmpty()) {
                    controlRef.child("button_for_led").setValueAsync(Long.valueOf(ledMode));
                }
                if (!brightness.isEmpty()) {
                    controlRef.child("candel_power_for_led").setValueAsync(Long.valueOf(brightness));
                }
                if (!fanMode.isEmpty()) {
                    controlRef.child("button_for_fan").setValueAsync(Long.valueOf(fanMode));
                }
            } else {
                session.sendMessage(new TextMessage("{\"error\": \"Sensor not found for user.\"}"));
            }
        } catch (IOException | JSONException e) {
            logger.error("Error while handling message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (sensorListeners.containsKey(session)) {
            DatabaseReference sensorRef = firebaseDatabase.getReference(sessions.get(session).getSecond());
            sensorRef.removeEventListener(sensorListeners.remove(session));
        }
        
        if (controlListeners.containsKey(session)) {
            DatabaseReference controlRef = firebaseDatabase.getReference(sessions.get(session).getSecond() + "_control");
            controlRef.removeEventListener(controlListeners.remove(session));
        }

        sessions.remove(session);
    }

    private DocumentSnapshot getSnapshotSafely(ApiFuture<DocumentSnapshot> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to retrieve document snapshot", e);
            return null;
        }
    }
} 
