package hcmut.smart_home.dto.sensor;

import java.util.Map;
import java.util.stream.Collectors;

public class SensorData {
    private long ledStatus;
    private long fanStatus;
    private long humidity;
    private long lightIntensity;
    private long temperature;
    private long timestamp;
    private boolean sendable = false;

    public SensorData() {
        this.ledStatus = 0;
        this.fanStatus = 0;
        this.humidity = 0;
        this.lightIntensity = 0;
        this.temperature = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public void updateData(Object data) {
        if (!(data instanceof Map<?, ?>)) {
            return;
        }

        Map<?, ?> rawMap = (Map<?, ?>) data;
        Map<String, Object> dataMap = rawMap.entrySet().stream()
            .filter(entry -> entry.getKey() instanceof String)
            .collect(Collectors.toMap(
                entry -> (String) entry.getKey(),
                Map.Entry::getValue
            ));

        this.humidity       = dataMap.get("humidity") != null 
                                ? (long) dataMap.get("humidity")        
                                : humidity;
        this.lightIntensity = dataMap.get("light_intensity") != null 
                                ? (long) dataMap.get("light_intensity") 
                                : lightIntensity;
        this.temperature    = dataMap.get("temperature") != null 
                                ? (long) dataMap.get("temperature")     
                                : temperature;
        this.timestamp      = dataMap.get("timestamp") != null 
                                ? (long) dataMap.get("timestamp")      
                                : timestamp;
        this.ledStatus      = dataMap.get("button_for_led") != null 
                                ? (long) dataMap.get("button_for_led")  
                                : ledStatus;
        this.fanStatus      = dataMap.get("button_for_fan") != null 
                                ? (long) dataMap.get("button_for_fan")  
                                : fanStatus;
    }

    public long getLedStatus() {
        return ledStatus;
    }

    public void setLedStatus(long ledStatus) {
        this.ledStatus = ledStatus;
    }

    public long getFanStatus() {
        return fanStatus;
    }

    public void setFanStatus(long fanStatus) {
        this.fanStatus = fanStatus;
    }

    public long getHumidity() {
        return humidity;
    }

    public void setHumidity(long humidity) {
        this.humidity = humidity;
    }

    public long getLightIntensity() {
        return lightIntensity;
    }

    public void setLightIntensity(long lightIntensity) {
        this.lightIntensity = lightIntensity;
    }

    public long getTemperature() {
        return temperature;
    }

    public void setTemperature(long temperature) {
        this.temperature = temperature;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("{\"led_status\": %d, \"fan_status\": %d, \"humidity\": %d, \"light_intensity\": %d, \"temperature\": %d, \"timestamp\": %d}",
                ledStatus, fanStatus, humidity, lightIntensity, temperature, timestamp);
    }

    public boolean isSendable() {
        return sendable;
    }

    public void setSendable() {
        this.sendable = true;
    }
}
