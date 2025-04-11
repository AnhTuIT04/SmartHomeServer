package hcmut.smart_home.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class UpdateModeConfigRequest {
    private String name;

    @Min(value = 0, message = "LED mode must be at least 0")
    @Max(value = 4, message = "LED mode must be at most 4")
    private Long ledMode;

    @Min(value = 0, message = "Brightness must be at least 0")
    @Max(value = 100, message = "Brightness must be at most 100")
    private Long brightness;

    @Min(value = 0, message = "Fan mode must be at least 0")
    @Max(value = 3, message = "Fan mode must be at most 3")
    private Long fanMode;

    public UpdateModeConfigRequest(String name, Long ledMode, Long brightness, Long fanMode) {
        this.name = name;
        this.ledMode = ledMode;
        this.brightness = brightness;
        this.fanMode = fanMode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getLedMode() {
        return ledMode;
    }

    public void setLedMode(Long ledMode) {
        this.ledMode = ledMode;
    }

    public Long getBrightness() {
        return brightness;
    }

    public void setBrightness(Long brightness) {
        this.brightness = brightness;
    }

    public Long getFanMode() {
        return fanMode;
    }

    public void setFanMode(Long fanMode) {
        this.fanMode = fanMode;
    }
    
}
