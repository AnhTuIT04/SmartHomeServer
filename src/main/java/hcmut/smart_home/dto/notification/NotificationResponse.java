package hcmut.smart_home.dto.notification;

public class NotificationResponse {
    public enum Type { HUMIDITY, TEMPERATURE, LIGHT_INTENSITY, REQUEST_ACCESS }
    public enum Mode { WARN, FORCE, REQUEST_ACCESS }

    private String id;
    private String sensorId;
    private String message;
    private Type type;
    private Mode mode;
    private long timestamp;

    public NotificationResponse(String id, String sensorId, String message, Type type, Mode mode) {
        this.id = id;
        this.sensorId = sensorId;
        this.message = message;
        this.type = type;
        this.mode = mode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }    

    @Override
    public String toString() {
        return "{\"id\": \"" + id + "\", \"sensorId\": \"" + sensorId + "\", \"message\": \"" + message + "\", \"type\": \"" + type + "\", \"mode\": \"" + mode + "\", \"timestamp\": " + timestamp + "}";
    }
}
