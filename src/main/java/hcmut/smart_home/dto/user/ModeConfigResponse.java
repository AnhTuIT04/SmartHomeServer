package hcmut.smart_home.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "ModeConfigResponse", accessMode = Schema.AccessMode.READ_ONLY)
public class ModeConfigResponse {
    private String id;
    private String userId;
    private String name;
    private long ledMode;
    private long brightness;
    private long fanMode;

    public ModeConfigResponse(String id, String userId, String name, long ledMode, long brightness, long fanMode) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.ledMode = ledMode;
        this.brightness = brightness;
        this.fanMode = fanMode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
