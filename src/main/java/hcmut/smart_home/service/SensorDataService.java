package hcmut.smart_home.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.database.ChildEventListener;
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
import hcmut.smart_home.handler.WebSocketNotificationHandler;

@Service
public class SensorDataService {

    private static final Logger logger = LoggerFactory.getLogger(SensorDataService.class);
    private static final long COOLDOWN_SECONDS = 0;
    private final Map<String, Instant> lastNotificationTimes = new ConcurrentHashMap<>();
    private final Map<String, ValueEventListener> sensorListeners = new ConcurrentHashMap<>();

    private final WebSocketNotificationHandler webSocketNotificationHandler;
    private final NotificationService notificationService;
    private final Firestore firestore;
    private final FirebaseDatabase firebaseDatabase;

    public SensorDataService(NotificationService notificationService, Firestore firestore,
                             FirebaseDatabase firebaseDatabase, WebSocketNotificationHandler webSocketNotificationHandler) {
        this.notificationService = notificationService;
        this.firestore = firestore;
        this.firebaseDatabase = firebaseDatabase;
        this.webSocketNotificationHandler = webSocketNotificationHandler;
    }

    @PostConstruct
    public void startListening() {
        DatabaseReference sensorsRef = firebaseDatabase.getReference("data");

        sensorsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String sensorId = snapshot.getKey();
                listenToSensor(sensorId);
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                String sensorId = snapshot.getKey();
                stopListeningToSensor(sensorId);
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {
                logger.error("Error while listening to sensors: {}", error.getMessage());
            }
        });
    }

    private void listenToSensor(String sensorId) {
        DatabaseReference sensorRef = firebaseDatabase.getReference("data/" + sensorId);
        SensorData data = new SensorData();

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                data.updateData(snapshot.getValue());
                checkThreshold(sensorId, data);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logger.error("Error listening to sensor {}: {}", sensorId, error.getMessage());
            }
        };

        sensorRef.addValueEventListener(listener);
        sensorListeners.put(sensorId, listener);
        logger.info("Started listening to sensor: {}", sensorId);
    }

    private void stopListeningToSensor(String sensorId) {
        DatabaseReference sensorRef = firebaseDatabase.getReference("data/" + sensorId);
        ValueEventListener listener = sensorListeners.remove(sensorId);
        if (listener != null) {
            sensorRef.removeEventListener(listener);
            lastNotificationTimes.remove(sensorId);
            logger.info("Stopped listening to sensor: {}", sensorId);
        }
    }

    private void checkThreshold(String sensorId, SensorData data) {
        Instant now = Instant.now();
        Instant lastNotification = lastNotificationTimes.get(sensorId);
    
        if (lastNotification == null || now.isAfter(lastNotification.plusSeconds(COOLDOWN_SECONDS))) {
            try {
                DocumentSnapshot sensorSnapshot = firestore.collection("sensors").document(sensorId).get().get();
                SensorInfoResponse sensorInfo = new SensorInfoResponse(sensorSnapshot.getData());
    
                double light = data.getLightIntensity();
                double humidity = data.getHumidity();
                double temp = data.getTemperature();
    
                // Create a new notification
                NotificationResponse notification = notificationService.createNotification(sensorId);
    
                // Check for force conditions and add to notification
                if (light > sensorInfo.getLightForceUpper()) {
                    notification.addDetail(Type.LIGHT_INTENSITY, Mode.FORCE);
                    forceControl(sensorId, Type.LIGHT_INTENSITY, true);
                } else if (light < sensorInfo.getLightForceLower()) {
                    notification.addDetail(Type.LIGHT_INTENSITY, Mode.FORCE);
                    forceControl(sensorId, Type.LIGHT_INTENSITY, false);
                }
    
                if (humidity > sensorInfo.getHumForceUpper()) {
                    notification.addDetail(Type.HUMIDITY, Mode.FORCE);
                    forceControl(sensorId, Type.HUMIDITY, true);
                } else if (humidity < sensorInfo.getHumForceLower()) {
                    notification.addDetail(Type.HUMIDITY, Mode.FORCE);
                    forceControl(sensorId, Type.HUMIDITY, false);
                }
    
                if (temp > sensorInfo.getTempForceUpper()) {
                    notification.addDetail(Type.TEMPERATURE, Mode.FORCE);
                    forceControl(sensorId, Type.TEMPERATURE, true);
                } else if (temp < sensorInfo.getTempForceLower()) {
                    notification.addDetail(Type.TEMPERATURE, Mode.FORCE);
                    forceControl(sensorId, Type.TEMPERATURE, false);
                }
    
                // Check for warning conditions and add to notification
                if (!hasForceType(notification, Type.LIGHT_INTENSITY) &&
                    (light > sensorInfo.getLightWarnUpper() || light < sensorInfo.getLightWarnLower())) {
                    notification.addDetail(Type.LIGHT_INTENSITY, Mode.WARN);
                }
                if (!hasForceType(notification, Type.HUMIDITY) &&
                    (humidity > sensorInfo.getHumWarnUpper() || humidity < sensorInfo.getHumWarnLower())) {
                    notification.addDetail(Type.HUMIDITY, Mode.WARN);
                }
                if (!hasForceType(notification, Type.TEMPERATURE) &&
                    (temp > sensorInfo.getTempWarnUpper() || temp < sensorInfo.getTempWarnLower())) {
                    notification.addDetail(Type.TEMPERATURE, Mode.WARN);
                }
    
                // Send notification if there are any details
                if (!notification.getDetails().isEmpty()) {
                    sendNotification(notification);
                    lastNotificationTimes.put(sensorId, now);
                }
    
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while checking threshold for sensor {}: ", sensorId, e.getMessage());
            } catch (ExecutionException e) {
                logger.error("Error executing Firestore query for sensor {}: ", sensorId, e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error checking threshold for sensor {}: ", sensorId, e.getMessage());
            }
        } else {
            logger.info("Sensor {} is in cooldown, skipping check.", sensorId);
        }
    }

    private void forceControl(String sensorId, Type type, boolean isUpper) {
        DatabaseReference controlRef = firebaseDatabase.getReference("control/" + sensorId);

        try {
            switch (type) {
                case TEMPERATURE -> adjustFanLevel(controlRef, isUpper);
                case HUMIDITY -> controlRef.child("button_for_fan").setValueAsync(isUpper ? 2 : 0);
                case LIGHT_INTENSITY -> {
                    adjustLedBrightness(controlRef, isUpper);
                    adjustLedLevel(controlRef, isUpper);
                }
                default -> logger.warn("Unsupported type for force control: {}", type);
            }
        } catch (Exception e) {
            logger.error("Error controlling sensor {}: ", sensorId, e.getMessage());
        }
    }

    /**
     * Force control for LED, fan, and brightness.
     * @param sensorId the ID of the sensor
     * @param ledMode the mode for LED (0-4)
     * @param fanMode the mode for fan (0-3)
     * @param brightness the brightness level (10-100)
     */
    public void forceControl(String sensorId, long ledMode, long fanMode, long brightness) {
        DatabaseReference controlRef = firebaseDatabase.getReference("control/" + sensorId);
        try {
            controlRef.child("button_for_led").setValueAsync(ledMode);
            controlRef.child("button_for_fan").setValueAsync(fanMode);
            controlRef.child("candel_power_for_led").setValueAsync(brightness);
            logger.info("Forced control for sensor {}: ledMode={}, fanMode={}, brightness={}", sensorId, ledMode, fanMode, brightness);
        } catch (Exception e) {
            logger.error("Error controlling sensor {}: ", sensorId, e.getMessage());
        }
    }

    private boolean hasForceType(NotificationResponse notification, Type type) {
        return notification.getDetails().stream()
                .anyMatch(pair -> pair.getType() == type && pair.getMode() == Mode.FORCE);
    }

    private void adjustFanLevel(DatabaseReference controlRef, boolean isUpper) {
        controlRef.child("button_for_fan").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Integer currentLevel = snapshot.getValue(Integer.class);
                int newLevel = (currentLevel == null ? 0 : currentLevel) + (isUpper ? 1 : -1);
                newLevel = Math.max(0, Math.min(3, newLevel));
                controlRef.child("button_for_fan").setValueAsync(newLevel);
                logger.info("Fan level changed to: {} for sensor: {}", newLevel, controlRef.getKey());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logger.error("Error adjusting fan level: {}", error.getMessage());
            }
        });
    }

    private void adjustLedBrightness(DatabaseReference controlRef, boolean isUpper) {
        controlRef.child("candel_power_for_led").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Integer brightness = snapshot.getValue(Integer.class);
                int newBrightness = (brightness == null ? 50 : brightness) + (isUpper ? -10 : 10);
                newBrightness = Math.max(10, Math.min(100, newBrightness));
                controlRef.child("candel_power_for_led").setValueAsync(newBrightness);
                logger.info("LED brightness changed to: {} for sensor: {}", newBrightness, controlRef.getKey());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logger.error("Error adjusting LED brightness: {}", error.getMessage());
            }
        });
    }

    private void adjustLedLevel(DatabaseReference controlRef, boolean isUpper) {
        controlRef.child("button_for_led").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Integer level = snapshot.getValue(Integer.class);
                int newLevel = (level == null ? 0 : level) + (isUpper ? -1 : 1);
                newLevel = Math.max(0, Math.min(4, newLevel));
                controlRef.child("button_for_led").setValueAsync(newLevel);
                logger.info("LED level changed to: {} for sensor: {}", newLevel, controlRef.getKey());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logger.error("Error adjusting LED level: {}", error.getMessage());
            }
        });
    }

    private void sendNotification(NotificationResponse notification) {
        String message = notification.toString();
        webSocketNotificationHandler.sendNotification(notification.getSensorId(), message);
        notificationService.saveNotification(notification);
    }
}