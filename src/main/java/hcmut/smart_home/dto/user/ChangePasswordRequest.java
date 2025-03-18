package hcmut.smart_home.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(title = "ChangePasswordRequest", accessMode = Schema.AccessMode.WRITE_ONLY)
public class ChangePasswordRequest {

    @Size(min = 6, message = "Current password must be at least 6 characters")
    @NotBlank(message = "Current password is required")
    private String currPassword;

    @Size(min = 6, message = "New password must be at least 6 characters")
    @NotBlank(message = "New password is required")
    private String newPassword;

    public String getCurrPassword() {
        return currPassword;
    }

    public void setCurrPassword(String currPassword) {
        this.currPassword = currPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
