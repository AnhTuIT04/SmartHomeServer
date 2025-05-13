package hcmut.smart_home.dto.user;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(title = "FaceIDRequest", accessMode = Schema.AccessMode.WRITE_ONLY)
public class FaceIDRequest {
    @NotNull(message = "Embedding map is required")
    private List<Double> embedding;

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }
}
