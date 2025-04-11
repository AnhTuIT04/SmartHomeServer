package hcmut.smart_home.handler;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

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

import hcmut.smart_home.util.Jwt;

@Component
public class WebSocketNotificationHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationHandler.class);
    private final ConcurrentHashMap<WebSocketSession, String> sessionSensorMap = new ConcurrentHashMap<>();
    private final Jwt jwt;
    private final Firestore firestore;

    public WebSocketNotificationHandler(Jwt jwt, Firestore firestore) {
        this.jwt = jwt;
        this.firestore = firestore;
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

            DocumentSnapshot sensorSnapshot = getSnapshotSafely(firestore.collection("sensors").document(sensorId).get());
            if (sensorSnapshot == null || !sensorSnapshot.exists()) {
                sendAndClose(session, "{\"error\": \"Sensor not found\"}", CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            sessionSensorMap.put(session, sensorId);
            logger.info("New WebSocket connection for sensorId: {} , session: {}", sensorId, session.getId());

        } catch (Exception e) {
            logger.error("Error while establishing connection for session {}: ", session.getId(), e);
            sendAndClose(session, "{\"error\": \"Internal server error\"}", CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionSensorMap.remove(session);
        logger.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket error for session {}: ", session.getId(), exception);
        sessionSensorMap.remove(session);
    }

    public void sendNotification(String sensorId, String message) {
        for (WebSocketSession session : sessionSensorMap.keySet()) {
            String assignedSensorId = sessionSensorMap.get(session);
            if (session.isOpen() && sensorId.equals(assignedSensorId)) {
                try {
                    session.sendMessage(new TextMessage(message));
                    logger.info("Sent notification to session {} for sensorId {}: {}", session.getId(), sensorId, message);
                } catch (IOException e) {
                    logger.error("Error sending message to session {}: ", session.getId(), e);
                }
            }
        }
    }

    public void sendNotificationToUser(String userId, String message) {
        for (WebSocketSession session : sessionSensorMap.keySet()) {
            String assignedUserId = jwt.extractId(extractTokenFromUri(session.getUri()));
            if (session.isOpen() && userId.equals(assignedUserId)) {
                try {
                    session.sendMessage(new TextMessage(message));
                    logger.info("Sent notification to user {}: {}", userId, message);
                } catch (IOException e) {
                    logger.error("Error sending message to user {}: ", userId, e);
                }
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