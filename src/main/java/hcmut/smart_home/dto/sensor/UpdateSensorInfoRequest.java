package hcmut.smart_home.dto.sensor;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "UpdateSensorInfoRequest", accessMode = Schema.AccessMode.WRITE_ONLY)
public class UpdateSensorInfoRequest {
    private Double humWarnUpper;
    private Double humWarnLower;
    private Double tempWarnUpper;
    private Double tempWarnLower;
    private Double lightWarnUpper;
    private Double lightWarnLower;
    private Double humForceUpper;
    private Double humForceLower;
    private Double tempForceUpper;
    private Double tempForceLower;
    private Double lightForceUpper;
    private Double lightForceLower;
    
    public Double getHumWarnUpper() {
        return humWarnUpper;
    }
    public void setHumWarnUpper(double humWarnUpper) {
        this.humWarnUpper = humWarnUpper;
    }
    public Double getHumWarnLower() {
        return humWarnLower;
    }
    public void setHumWarnLower(double humWarnLower) {
        this.humWarnLower = humWarnLower;
    }
    public Double getTempWarnUpper() {
        return tempWarnUpper;
    }
    public void setTempWarnUpper(double tempWarnUpper) {
        this.tempWarnUpper = tempWarnUpper;
    }
    public Double getTempWarnLower() {
        return tempWarnLower;
    }
    public void setTempWarnLower(double tempWarnLower) {
        this.tempWarnLower = tempWarnLower;
    }
    public Double getLightWarnUpper() {
        return lightWarnUpper;
    }
    public void setLightWarnUpper(double lightWarnUpper) {
        this.lightWarnUpper = lightWarnUpper;
    }
    public Double getLightWarnLower() {
        return lightWarnLower;
    }
    public void setLightWarnLower(double lightWarnLower) {
        this.lightWarnLower = lightWarnLower;
    }
    public Double getHumForceUpper() {
        return humForceUpper;
    }
    public void setHumForceUpper(double humForceUpper) {
        this.humForceUpper = humForceUpper;
    }
    public Double getHumForceLower() {
        return humForceLower;
    }
    public void setHumForceLower(double humForceLower) {
        this.humForceLower = humForceLower;
    }
    public Double getTempForceUpper() {
        return tempForceUpper;
    }
    public void setTempForceUpper(double tempForceUpper) {
        this.tempForceUpper = tempForceUpper;
    }
    public Double getTempForceLower() {
        return tempForceLower;
    }
    public void setTempForceLower(double tempForceLower) {
        this.tempForceLower = tempForceLower;
    }
    public Double getLightForceUpper() {
        return lightForceUpper;
    }
    public void setLightForceUpper(double lightForceUpper) {
        this.lightForceUpper = lightForceUpper;
    }
    public Double getLightForceLower() {
        return lightForceLower;
    }
    public void setLightForceLower(double lightForceLower) {
        this.lightForceLower = lightForceLower;
    }
}
