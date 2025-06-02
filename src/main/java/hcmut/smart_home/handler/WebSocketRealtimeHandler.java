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

    private static final Logger logger = LoggerFactory.getLogger(WebSocketRealtimeHandler.class);
    private final ConcurrentHashMap<WebSocketSession, Pair<String, String>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocketSession, ValueEventListener> sensorListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocketSession, ValueEventListener> controlListeners = new ConcurrentHashMap<>();

    private final Jwt jwt;
    private final Firestore firestore;
    private final FirebaseDatabase firebaseDatabase;

    public WebSocketRealtimeHandler(Jwt jwt, Firestore firestore, FirebaseDatabase firebaseDatabase) {
        this.jwt = jwt;
        this.firestore = firestore;
        this.firebaseDatabase = firebaseDatabase;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String token = extractTokenFromUri(session.getUri());
            if (token == null || !jwt.validateAccessToken(token)) {
                sendAndClose(session, "{\"error\": \"Unauthorized\"}", CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String userId = jwt.extractId(token);
            DocumentSnapshot userSnapshot = getSnapshotSafely(firestore.collection("users").document(userId).get());
            if (userSnapshot == null || !userSnapshot.exists()) {
                sendAndClose(session, "{\"error\": \"User not found\"}", CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String sensorId = userSnapshot.getString("sensorId");
            if (sensorId == null) {
                sendAndClose(session, "{\"error\": \"Sensor not found\"}", CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            sessions.put(session, new Pair<>(userId, sensorId));
            setupRealtimeListeners(session, sensorId);
            logger.info("New WebSocket connection for userId: {}, sensorId: {}, session: {}", userId, sensorId, session.getId());

        } catch (Exception e) {
            logger.error("Error while establishing connection for session {}: ", session.getId(), e);
            sendAndClose(session, "{\"error\": \"Internal server error\"}", CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            logger.info("_____________________________", payload);
            JSONObject jsonMessage = new JSONObject(payload);
            String ledMode = jsonMessage.optString("led_mode");
            String brightness = jsonMessage.optString("led_brightness");
            String fanMode = jsonMessage.optString("fan_mode");

            Pair<String, String> sessionData = sessions.get(session);
            if (sessionData == null) {
                sendAndClose(session, "{\"error\": \"Session not found\"}", CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String sensorId = sessionData.getSecond();
            DatabaseReference controlRef = firebaseDatabase.getReference("control/" + sensorId);

            if (!ledMode.isEmpty()) {
                controlRef.child("button_for_led").setValueAsync(Long.valueOf(ledMode));
            }
            if (!brightness.isEmpty()) {
                controlRef.child("candel_power_for_led").setValueAsync(Long.valueOf(brightness));
            }
            if (!fanMode.isEmpty()) {
                controlRef.child("button_for_fan").setValueAsync(Long.valueOf(fanMode));
            }

            logger.info("Updated control data for sensorId: {}", sensorId);
        } catch (JSONException e) {
            logger.error("Error handling message for session {}: ", session.getId(), e);
            sendAndClose(session, "{\"error\": \"Invalid message format\"}", CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanupSession(session);
        logger.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket error for session {}: ", session.getId(), exception);
        cleanupSession(session);
    }

    private void setupRealtimeListeners(WebSocketSession session, String sensorId) {
        SensorData data = new SensorData();
        DatabaseReference sensorRef = firebaseDatabase.getReference("data/" + sensorId);
        DatabaseReference controlRef = firebaseDatabase.getReference("control/" + sensorId);

        ValueEventListener sensorListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    data.updateData(snapshot.getValue());
                    if (data.isSendable()) {
                        sendDataToClient(session, data);
                    } else {
                        data.setSendable();
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
                    if (data.isSendable()) {
                        sendDataToClient(session, data);
                    } else {
                        data.setSendable();
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
    }

    private void sendDataToClient(WebSocketSession session, SensorData data) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(data.toString()));
                logger.debug("Sent data to session {}: {}", session.getId(), data);
            }
        } catch (IOException e) {
            logger.error("Error sending data to session {}: ", session.getId(), e);
        }
    }

    private void handleDatabaseError(WebSocketSession session, DatabaseError error) {
        logger.error("Database error for session {}: {}", session.getId(), error.getMessage());
        sendAndClose(session, "{\"error\": \"" + error.getMessage() + "\"}", CloseStatus.SERVER_ERROR);
    }

    private void cleanupSession(WebSocketSession session) {
        Pair<String, String> sessionData = sessions.remove(session);
        if (sessionData != null) {
            String sensorId = sessionData.getSecond();
            ValueEventListener sensorListener = sensorListeners.remove(session);
            if (sensorListener != null) {
                firebaseDatabase.getReference(sensorId).removeEventListener(sensorListener);
            }
            ValueEventListener controlListener = controlListeners.remove(session);
            if (controlListener != null) {
                firebaseDatabase.getReference(sensorId + "_control").removeEventListener(controlListener);
            }
        }
    }

    private String extractTokenFromUri(URI uri) {
        if (uri == null || uri.getQuery() == null || !uri.getQuery().startsWith("token=")) {
            return null;
        }
        String[] queryParts = uri.getQuery().split("token=");
        return queryParts.length > 1 ? queryParts[1] : null;
    }

    private void sendAndClose(WebSocketSession session, String message, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
                session.close(status);
            }
        } catch (IOException e) {
            logger.error("Error sending message or closing session {}: ", session.getId(), e);
        }
    }

    private DocumentSnapshot getSnapshotSafely(ApiFuture<DocumentSnapshot> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to retrieve document snapshot: ", e);
            return null;
        }
    }
}