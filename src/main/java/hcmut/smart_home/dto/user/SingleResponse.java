package hcmut.smart_home.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "SingleResponse", accessMode = Schema.AccessMode.READ_ONLY)
public class SingleResponse {
    private String message;

    public SingleResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}