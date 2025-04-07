package hcmut.smart_home.dto.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationResponse {
    public enum Type { HUMIDITY, TEMPERATURE, LIGHT_INTENSITY, REQUEST_ACCESS }
    public enum Mode { WARN, FORCE, REQUEST_ACCESS }

    private String id;
    private String sensorId;
    private List<Detail> details;
    private long timestamp;

    public NotificationResponse(String id, String sensorId) {
        this.id = id;
        this.sensorId = sensorId;
        this.details = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public NotificationResponse(String id, String sensorId, Type type, Mode mode) {
        this.id = id;
        this.sensorId = sensorId;
        this.details = new ArrayList<>();
        this.details.add(new Detail(type, mode));
        this.timestamp = System.currentTimeMillis();
    }

    public void addDetail(Type type, Mode mode) {
        this.details.add(new Detail(type, mode));
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

    public List<Detail> getDetails() {
        return details;
    }

    public void setDetails(List<Detail> details) {
        this.details = details;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }    

    @Override
    public String toString() {
        try {
            List<String> detailsJson = details.stream()
                    .map(detail -> String.format("{\"type\": \"%s\", \"mode\": \"%s\"}", 
                            detail.getType().name(), detail.getMode().name()))
                    .collect(Collectors.toList());
            String detailsString = "[" + String.join(", ", detailsJson) + "]";
            return String.format("{\"id\": \"%s\", \"sensorId\": \"%s\", \"details\": %s, \"timestamp\": %d}",
                    id, sensorId, detailsString, timestamp);
        } catch (Exception e) {
            return "{\"error\": \"Failed to serialize notification: " + e.getMessage() + "\"}";
        }
    }

    public static class Detail {
        private Type type;
        private Mode mode;

        public Detail(Type type, Mode mode) {
            this.type = type;
            this.mode = mode;
        }

        public Type getType() { return type; }
        public void setType(Type type) { this.type = type; }
        public Mode getMode() { return mode; }
        public void setMode(Mode mode) { this.mode = mode; }
    }
}
