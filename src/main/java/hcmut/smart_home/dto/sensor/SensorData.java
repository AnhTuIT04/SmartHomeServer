package hcmut.smart_home.dto.sensor;

import java.util.Map;
import java.util.stream.Collectors;

public class SensorData {
    private long ledMode;
    private long brightness;
    private long fanMode;
    private double humidity;
    private double lightIntensity;
    private double temperature;
    private long timestamp;
    private boolean sendable = false;

    public SensorData() {
        this.ledMode = 0;
        this.fanMode = 0;
        this.brightness = 0;
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
                                ? ((Number) dataMap.get("humidity")).doubleValue()
                                : humidity;
        this.lightIntensity = dataMap.get("light_intensity") != null 
                                ? ((Number) dataMap.get("light_intensity")).doubleValue() 
                                : lightIntensity;
        this.temperature    = dataMap.get("temperature") != null 
                                ? ((Number) dataMap.get("temperature")).doubleValue()
                                : temperature;
        this.timestamp      = dataMap.get("timestamp") != null 
                                ? (long) dataMap.get("timestamp")      
                                : timestamp;
        this.ledMode        = dataMap.get("button_for_led") != null 
                                ? (long) dataMap.get("button_for_led")  
                                : ledMode;
        this.fanMode        = dataMap.get("button_for_fan") != null 
                                ? (long) dataMap.get("button_for_fan")  
                                : fanMode;
        this.brightness     = dataMap.get("candel_power_for_led") != null
                                ? (long) dataMap.get("candel_power_for_led")
                                : brightness;
    }

    public long getLedMode() {
        return ledMode;
    }

    public void setLedMode(long ledMode) {
        this.ledMode = ledMode;
    }

    public long getFanMode() {
        return fanMode;
    }

    public void setFanMode(long fanMode) {
        this.fanMode = fanMode;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getLightIntensity() {
        return lightIntensity;
    }

    public void setLightIntensity(double lightIntensity) {
        this.lightIntensity = lightIntensity;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
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
        return String.format("{\"led_mode\": %d, \"led_brightness\": %d, \"fan_mode\": %d, \"humidity\": %f, \"light_intensity\": %f, \"temperature\": %f, \"timestamp\": %d}",
                ledMode, brightness, fanMode, humidity, lightIntensity, temperature, timestamp);
    }

    public boolean isSendable() {
        return sendable;
    }

    public void setSendable() {
        this.sendable = true;
    }

    public long getBrightness() {
        return brightness;
    }

    public void setBrightness(long brightness) {
        this.brightness = brightness;
    }
}
