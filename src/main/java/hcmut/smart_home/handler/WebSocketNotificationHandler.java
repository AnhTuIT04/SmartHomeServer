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

import hcmut.smart_home.dto.notification.NotificationResponse;
import hcmut.smart_home.dto.notification.NotificationResponse.Mode;
import hcmut.smart_home.dto.notification.NotificationResponse.Type;
import hcmut.smart_home.dto.sensor.SensorData;
import hcmut.smart_home.dto.sensor.SensorInfoResponse;
import hcmut.smart_home.service.NotificationService;
import hcmut.smart_home.util.Jwt;
import hcmut.smart_home.util.Pair;

@Component
public class WebSocketNotificationHandler extends TextWebSocketHandler {
    private final Jwt jwt;
    private final Firestore firestore;
    private final FirebaseDatabase firebaseDatabase;
    private final NotificationService notificationService;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationHandler.class);

    private final ConcurrentHashMap<WebSocketSession, Pair<String, String>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocketSession, ValueEventListener> sensorListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocketSession, ConcurrentHashMap<Type, Long>> lastNotificationTimestamps = new ConcurrentHashMap<>();

    public WebSocketNotificationHandler(Jwt jwt, Firestore firestore, FirebaseDatabase firebaseDatabase, NotificationService notificationService) {
        this.jwt = jwt;
        this.firestore = firestore;
        this.firebaseDatabase = firebaseDatabase;
        this.notificationService = notificationService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null || uri.getQuery() == null || !uri.getQuery().startsWith("token=")) {
                sendAndClose(session, "{\"error\": \"Unauthorized\"}", CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String token = uri.getQuery().split("token=")[1];
            if (!jwt.validateAccessToken(token)) {
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

            DocumentSnapshot sensorSnapshot = getSnapshotSafely(firestore.collection("sensors").document(sensorId).get());
            if (sensorSnapshot == null || !sensorSnapshot.exists()) {
                sendAndClose(session, "{\"error\": \"Sensor not found\"}", CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            sessions.put(session, new Pair<>(userId, sensorId));
            SensorInfoResponse sensorInfo = new SensorInfoResponse(sensorSnapshot.getData());
            SensorData data = new SensorData();
            DatabaseReference sensorRef = firebaseDatabase.getReference(sensorId);

            ValueEventListener sensorListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        data.updateData(snapshot.getValue());
                        checkAndNotify(session, Type.HUMIDITY, data.getHumidity(), sensorInfo.getHumWarnLower(), sensorInfo.getHumWarnUpper(), sensorInfo.getHumForceLower(), sensorInfo.getHumForceUpper());
                        checkAndNotify(session, Type.TEMPERATURE, data.getTemperature(), sensorInfo.getTempWarnLower(), sensorInfo.getTempWarnUpper(), sensorInfo.getTempForceLower(), sensorInfo.getTempForceUpper());
                        checkAndNotify(session, Type.LIGHT_INTENSITY, data.getLightIntensity(), sensorInfo.getLightWarnLower(), sensorInfo.getLightWarnUpper(), sensorInfo.getLightForceLower(), sensorInfo.getLightForceUpper());
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    handleDatabaseError(session, error);
                }
            };

            sensorRef.addValueEventListener(sensorListener);
            sensorListeners.put(session, sensorListener);

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

    private void checkAndNotify(WebSocketSession session, Type type, double value, 
                                double warnLower, double warnUpper,
                                double forceLower, double forceUpper) {

        if (value < warnLower || value > warnUpper) {
            sendNotificationToClient(session, type, Mode.WARN, type.name() + " out of range: " + value);
        }

        if (value < forceLower || value > forceUpper) {
            sendNotificationToClient(session, type, Mode.FORCE, type.name() + " out of range: " + value);
        }
    }

    private void sendNotificationToClient(WebSocketSession session, Type type, Mode mode, String message) {
        try {
            if (!shouldSendNotification(session, type, 60_000)) return;

            String sensorId = sessions.get(session).getSecond();
            NotificationResponse notification = notificationService.createNotification(sensorId, message, type, mode);
            session.sendMessage(new TextMessage(notification.toString()));
        } catch (IOException e) {
            logger.error("Error while sending notification to client: ", e);
        }
    }

    private boolean shouldSendNotification(WebSocketSession session, Type type, long cooldownMillis) {
        long now = System.currentTimeMillis();
        lastNotificationTimestamps.putIfAbsent(session, new ConcurrentHashMap<>());
        ConcurrentHashMap<Type, Long> timestamps = lastNotificationTimestamps.get(session);

        Long lastSent = timestamps.getOrDefault(type, 0L);
        if (now - lastSent >= cooldownMillis) {
            timestamps.put(type, now);
            return true;
        }
        return false;
    }

    private void handleDatabaseError(WebSocketSession session, DatabaseError error) {
        try {
            session.sendMessage(new TextMessage("{\"error\": \"" + error.getMessage() + "\"}"));
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            logger.error("Error while closing session: ", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JSONObject jsonMessage = new JSONObject(message.getPayload());

            String ledMode = jsonMessage.optString("led_mode");
            String brightness = jsonMessage.optString("led_brightness");
            String fanMode = jsonMessage.optString("fan_mode");

            String sensorId = sessions.get(session).getSecond();
            if (sensorId == null) {
                session.sendMessage(new TextMessage("{\"error\": \"Sensor not found for user.\"}"));
                return;
            }

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

        } catch (IOException | JSONException e) {
            logger.error("Error while handling message: ", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ValueEventListener listener = sensorListeners.remove(session);
        if (listener != null) {
            String sensorId = sessions.getOrDefault(session, new Pair<>("", "")).getSecond();
            if (sensorId != null) {
                firebaseDatabase.getReference(sensorId).removeEventListener(listener);
            }
        }

        sessions.remove(session);
        lastNotificationTimestamps.remove(session);
    }

    private void sendAndClose(WebSocketSession session, String message, CloseStatus status) throws IOException {
        session.sendMessage(new TextMessage(message));
        session.close(status);
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
