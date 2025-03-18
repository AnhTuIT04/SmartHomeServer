package hcmut.smart_home.dto.sensor;

import com.google.cloud.Timestamp;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "PendingRequestResponse", accessMode = Schema.AccessMode.READ_ONLY)
public class PendingRequestResponse {
    private String id;
    private String sensorId;
    private String userId;
    private Timestamp createdAt;
    
    public PendingRequestResponse(String id, String sensorId, String userId, Timestamp createdAt) {
        this.id = id;
        this.sensorId = sensorId;
        this.userId = userId;
        this.createdAt = createdAt;
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
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
