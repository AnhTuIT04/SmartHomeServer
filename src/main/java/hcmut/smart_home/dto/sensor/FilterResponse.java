package hcmut.smart_home.dto.sensor;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "FilterResponse", accessMode = Schema.AccessMode.READ_ONLY)
public class FilterResponse {
    private String label;
    private double data;

    public FilterResponse(String label, double data) {
        this.label = label;
        this.data = data;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getData() {
        return data;
    }

    public void setData(double data) {
        this.data = data;
    }
    
}
