package hcmut.smart_home.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Schema(title = "CreateFaceIDRequest", accessMode = Schema.AccessMode.WRITE_ONLY)
public class CreateFaceIDRequest {
    @NotNull(message = "Embedded map is required")
    private Map<String, Double> embedded;

    public Map<String, Double> getEmbedded() {
        return embedded;
    }

    public void setEmbedded(Map<String, Double> embedded) {
        this.embedded = embedded;
    }
}
