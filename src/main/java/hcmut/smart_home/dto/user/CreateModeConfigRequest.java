package hcmut.smart_home.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(title = "CreateModeConfigRequest", accessMode = Schema.AccessMode.WRITE_ONLY)
public class CreateModeConfigRequest {
    @NotNull(message = "Name cannot be null")
    private String name;

    @NotNull(message = "LED mode cannot be null")
    @Min(value = 0, message = "LED mode must be at least 0")
    @Max(value = 4, message = "LED mode must be at most 4")
    private long ledMode;

    @NotNull(message = "Brightness cannot be null")
    @Min(value = 0, message = "Brightness must be at least 0")
    @Max(value = 100, message = "Brightness must be at most 100")
    private long brightness;

    @NotNull(message = "Fan mode cannot be null")
    @Min(value = 0, message = "Fan mode must be at least 0")
    @Max(value = 3, message = "Fan mode must be at most 3")
    private long fanMode;

    public CreateModeConfigRequest(String name, long ledMode, long brightness, long fanMode) {
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

    public long getLedMode() {
        return ledMode;
    }

    public void setLedMode(long ledMode) {
        this.ledMode = ledMode;
    }

    public long getBrightness() {
        return brightness;
    }

    public void setBrightness(long brightness) {
        this.brightness = brightness;
    }

    public long getFanMode() {
        return fanMode;
    }

    public void setFanMode(long fanMode) {
        this.fanMode = fanMode;
    }
}
