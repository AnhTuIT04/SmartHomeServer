package hcmut.smart_home.dto.sensor;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "SensorInfoResponse", accessMode = Schema.AccessMode.READ_ONLY)
public class SensorInfoResponse {
    private String id;
    private String ownerId;

    private double humWarnUpper;
    private double humWarnLower;
    private double tempWarnUpper;
    private double tempWarnLower;
    private double lightWarnUpper;
    private double lightWarnLower;

    private double humForceUpper;
    private double humForceLower;
    private double tempForceUpper;
    private double tempForceLower;
    private double lightForceUpper;
    private double lightForceLower;
    
    public SensorInfoResponse(String id, String ownerId) {
        this.id = id;
        this.ownerId = ownerId;

        this.humWarnUpper = 80L;
        this.humWarnLower = 20L;
        this.tempWarnUpper = 30L;
        this.tempWarnLower = 10L;
        this.lightWarnUpper = 1000L;
        this.lightWarnLower = 100L;
        this.humForceUpper = 90L;
        this.humForceLower = 10L;
        this.tempForceUpper = 35L;
        this.tempForceLower = 5L;
        this.lightForceUpper = 1500L;
        this.lightForceLower = 50L;
    }

    public SensorInfoResponse(String id, String ownerId, double humWarnUpper, double humWarnLower, double tempWarnUpper,
            double tempWarnLower, double lightWarnUpper, double lightWarnLower, double humForceUpper,
            double humForceLower, double tempForceUpper, double tempForceLower, double lightForceUpper,
            double lightForceLower) {
        this.id = id;
        this.ownerId = ownerId;
        this.humWarnUpper = humWarnUpper;
        this.humWarnLower = humWarnLower;
        this.tempWarnUpper = tempWarnUpper;
        this.tempWarnLower = tempWarnLower;
        this.lightWarnUpper = lightWarnUpper;
        this.lightWarnLower = lightWarnLower;
        this.humForceUpper = humForceUpper;
        this.humForceLower = humForceLower;
        this.tempForceUpper = tempForceUpper;
        this.tempForceLower = tempForceLower;
        this.lightForceUpper = lightForceUpper;
        this.lightForceLower = lightForceLower;
    }

    public SensorInfoResponse(Map<String, Object> data) {
        if (!(data instanceof Map<?, ?>)) {
            return;
        }

        Map<?, ?> rawMap = (Map<?, ?>) data;
        Map<String, Object> map = rawMap.entrySet().stream()
            .filter(entry -> entry.getKey() instanceof String)
            .collect(Collectors.toMap(
                entry -> (String) entry.getKey(),
                Map.Entry::getValue
            ));

        this.id = (String) map.get("id");
        this.ownerId = (String) map.get("ownerId");
        this.humWarnUpper = ((Number) map.get("humWarnUpper")).doubleValue();
        this.humWarnLower = ((Number) map.get("humWarnLower")).doubleValue();
        this.tempWarnUpper = ((Number) map.get("tempWarnUpper")).doubleValue();
        this.tempWarnLower = ((Number) map.get("tempWarnLower")).doubleValue();
        this.lightWarnUpper = ((Number) map.get("lightWarnUpper")).doubleValue();
        this.lightWarnLower = ((Number) map.get("lightWarnLower")).doubleValue();
        this.humForceUpper = ((Number) map.get("humForceUpper")).doubleValue();
        this.humForceLower = ((Number) map.get("humForceLower")).doubleValue();
        this.tempForceUpper = ((Number) map.get("tempForceUpper")).doubleValue();
        this.tempForceLower = ((Number) map.get("tempForceLower")).doubleValue();
        this.lightForceUpper = ((Number) map.get("lightForceUpper")).doubleValue();
        this.lightForceLower = ((Number) map.get("lightForceLower")).doubleValue();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("ownerId", ownerId);
        map.put("humWarnUpper", humWarnUpper);
        map.put("humWarnLower", humWarnLower);
        map.put("tempWarnUpper", tempWarnUpper);
        map.put("tempWarnLower", tempWarnLower);
        map.put("lightWarnUpper", lightWarnUpper);
        map.put("lightWarnLower", lightWarnLower);
        map.put("humForceUpper", humForceUpper);
        map.put("humForceLower", humForceLower);
        map.put("tempForceUpper", tempForceUpper);
        map.put("tempForceLower", tempForceLower);
        map.put("lightForceUpper", lightForceUpper);
        map.put("lightForceLower", lightForceLower);
        return map;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public double getHumWarnUpper() {
        return humWarnUpper;
    }

    public void setHumWarnUpper(double humWarnUpper) {
        this.humWarnUpper = humWarnUpper;
    }

    public double getHumWarnLower() {
        return humWarnLower;
    }

    public void setHumWarnLower(double humWarnLower) {
        this.humWarnLower = humWarnLower;
    }

    public double getTempWarnUpper() {
        return tempWarnUpper;
    }

    public void setTempWarnUpper(double tempWarnUpper) {
        this.tempWarnUpper = tempWarnUpper;
    }

    public double getTempWarnLower() {
        return tempWarnLower;
    }

    public void setTempWarnLower(double tempWarnLower) {
        this.tempWarnLower = tempWarnLower;
    }

    public double getLightWarnUpper() {
        return lightWarnUpper;
    }

    public void setLightWarnUpper(double lightWarnUpper) {
        this.lightWarnUpper = lightWarnUpper;
    }

    public double getLightWarnLower() {
        return lightWarnLower;
    }

    public void setLightWarnLower(double lightWarnLower) {
        this.lightWarnLower = lightWarnLower;
    }

    public double getHumForceUpper() {
        return humForceUpper;
    }

    public void setHumForceUpper(double humForceUpper) {
        this.humForceUpper = humForceUpper;
    }

    public double getHumForceLower() {
        return humForceLower;
    }

    public void setHumForceLower(double humForceLower) {
        this.humForceLower = humForceLower;
    }

    public double getTempForceUpper() {
        return tempForceUpper;
    }

    public void setTempForceUpper(double tempForceUpper) {
        this.tempForceUpper = tempForceUpper;
    }

    public double getTempForceLower() {
        return tempForceLower;
    }

    public void setTempForceLower(double tempForceLower) {
        this.tempForceLower = tempForceLower;
    }

    public double getLightForceUpper() {
        return lightForceUpper;
    }

    public void setLightForceUpper(double lightForceUpper) {
        this.lightForceUpper = lightForceUpper;
    }

    public double getLightForceLower() {
        return lightForceLower;
    }

    public void setLightForceLower(double lightForceLower) {
        this.lightForceLower = lightForceLower;
    }
}
