package hcmut.smart_home.dto.sensor;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "FilterResponse", accessMode = Schema.AccessMode.READ_ONLY)
public class FilterResponse {
    private String id;
    private double data;
    private long timestamp;

    public FilterResponse(String id, double data, long timestamp) {
        this.id = id;
        this.data = data;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getData() {
        return data;
    }

    public void setData(double data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
