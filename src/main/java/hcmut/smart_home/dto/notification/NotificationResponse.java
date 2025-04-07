package hcmut.smart_home.dto.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public NotificationResponse(Object data) {
        // Initialize fields with default values
        this.id = null;
        this.sensorId = null;
        this.timestamp = System.currentTimeMillis();
        this.details = new ArrayList<>();
    
        // Check if data is a Map
        if (!(data instanceof Map<?, ?>)) {
            return; // Exit if data is not a Map
        }
    
        // Convert to Map<?, ?>
        Map<?, ?> rawMap = (Map<?, ?>) data;
    
        // Safely convert to Map<String, Object>
        Map<String, Object> dataMap = rawMap.entrySet().stream()
            .filter(entry -> entry.getKey() instanceof String)
            .collect(Collectors.toMap(
                entry -> (String) entry.getKey(),
                Map.Entry::getValue
            ));
    
        // Populate fields with type-safe checks
        this.id = dataMap.get("id") instanceof String ? (String) dataMap.get("id") : null;
        this.sensorId = dataMap.get("sensorId") instanceof String ? (String) dataMap.get("sensorId") : null;
        this.timestamp = dataMap.get("timestamp") instanceof Long ? ((Number) dataMap.get("timestamp")).longValue() : System.currentTimeMillis();
    
        // Handle the details list with type safety
        Object detailsObj = dataMap.get("details");
        if (detailsObj instanceof List<?> rawDetailsList) {
            for (Object detail : rawDetailsList) {
                if (detail instanceof Map<?, ?> detailMap) {
                    try {
                        String typeStr = detailMap.get("type") instanceof String ? (String) detailMap.get("type") : null;
                        String modeStr = detailMap.get("mode") instanceof String ? (String) detailMap.get("mode") : null;
                        if (typeStr != null && modeStr != null) {
                            Type type = Type.valueOf(typeStr);
                            Mode mode = Mode.valueOf(modeStr);
                            this.details.add(new Detail(type, mode));
                        }
                    } catch (IllegalArgumentException e) {}
                }
            }
        }
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
