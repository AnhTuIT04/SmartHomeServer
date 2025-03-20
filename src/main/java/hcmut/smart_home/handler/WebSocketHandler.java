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

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import hcmut.smart_home.util.Jwt;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private final Jwt jwt;
    private final Firestore firestore;
    private final FirebaseDatabase firebaseDatabase;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

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

            String token = uri.getQuery().split("token=")[1]; // Get token from query parameter
            if (token == null || !jwt.validateAccessToken(token)) {
                session.sendMessage(new TextMessage("Error: Unauthorized"));
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            String userId = jwt.extractId(token);
            sessions.put(userId, session);

            // Get reference to the user document in Firestore
            DocumentSnapshot snapshot = firestore.collection("users").document(userId).get().get();
            if (!snapshot.exists()) {
                session.sendMessage(new TextMessage("Error: User not found"));
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            // Get the sensorId of the user
            String sensorId = snapshot.getString("sensorId");
            if (sensorId == null) {
                session.sendMessage(new TextMessage("Error: Sensor not found"));
                session.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            DatabaseReference sensorRef = firebaseDatabase.getReference(sensorId);
            sensorRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        try {
                            Object value = snapshot.getValue();
                            if (value != null) {
                                session.sendMessage(new TextMessage(value.toString()));
                            }
                        } catch (IOException e) {
                            logger.error("Error while establishing connection: " + e.getMessage());
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    try {
                        session.sendMessage(new TextMessage("Error: " + error.getMessage()));
                        session.close(CloseStatus.SERVER_ERROR);
                        logger.error("Error while reading data from sensor: " + error.getMessage());
                    } catch (IOException e) {
                        logger.error("Error while establishing connection: " + e.getMessage());
                    }
                }
            });
            
        } catch (IOException | ExecutionException e) {
            logger.error("Error while establishing connection: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error while establishing connection: " + e.getMessage());
        } 
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.values().remove(session);
    }
}
