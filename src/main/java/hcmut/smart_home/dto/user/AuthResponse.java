package hcmut.smart_home.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "AuthResponse", accessMode = Schema.AccessMode.READ_ONLY)
public class AuthResponse extends UserResponse {
    private final TokenResponse token;

    public AuthResponse(CreateUserRequest user, String id, String accessToken, String refreshToken) {
        super(id, user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhone(), user.getAvatar(), user.getSensorId());
        this.token = new TokenResponse(accessToken, refreshToken);
    }

    public AuthResponse(LoginUserRequest user, String id, String firstName, String lastName, String phone, String avatar, String sensorId, String accessToken, String refreshToken) {
        super(id, firstName, lastName, user.getEmail(), phone, avatar, sensorId);
        this.token = new TokenResponse(accessToken, refreshToken);
    }

    public String getAccessToken() {
        return token.getAccessToken();
    }

    public String getRefreshToken() {
        return token.getRefreshToken();
    }

    public void setAccessToken(String accessToken) {
        token.setAccessToken(accessToken);
    }

    public void setRefreshToken(String refreshToken) {
        token.setRefreshToken(refreshToken);
    }
}
