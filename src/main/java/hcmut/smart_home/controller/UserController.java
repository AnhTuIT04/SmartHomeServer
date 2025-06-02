package hcmut.smart_home.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import hcmut.smart_home.config.PublicEndpoint;
import hcmut.smart_home.dto.SingleResponse;
import hcmut.smart_home.dto.notification.NotificationResponse;
import hcmut.smart_home.dto.user.AuthResponse;
import hcmut.smart_home.dto.user.ChangePasswordRequest;
import hcmut.smart_home.dto.user.CreateModeConfigRequest;
import hcmut.smart_home.dto.user.CreateUserRequest;
import hcmut.smart_home.dto.user.LoginUserRequest;
import hcmut.smart_home.dto.user.ModeConfigResponse;
import hcmut.smart_home.dto.user.TokenRequest;
import hcmut.smart_home.dto.user.TokenResponse;
import hcmut.smart_home.dto.user.UpdateModeConfigRequest;
import hcmut.smart_home.dto.user.UpdateUserRequest;
import hcmut.smart_home.dto.user.UserResponse;
import hcmut.smart_home.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PublicEndpoint
    @PostMapping("/auth/register")
    @Operation(summary = "Create a new user account", tags = "Authentication")
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "201", description = "User record created successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "409", description = "User account with provided email/phone already exists",
            content = @Content()),
        @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content()),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content())
    })
    public ResponseEntity<AuthResponse> createUser(@Valid @RequestBody final CreateUserRequest user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(user));
    }

    @PublicEndpoint
    @PostMapping("/auth/login")
    @Operation(summary = "Login to user account", tags = "Authentication")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User logged in successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Email or password is incorrect",
                    content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content()) 
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody final LoginUserRequest user) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.login(user));
    }

    @PublicEndpoint
    @PostMapping(value = "/auth/login-face-id", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Login to user account using face ID", tags = "Authentication")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User logged in successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Face ID is incorrect",
                    content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<AuthResponse> loginWithFaceId(@RequestParam("image") MultipartFile image) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.loginWithFaceId(image));
    }

    @PublicEndpoint
    @PostMapping("/auth/refresh")
    @Operation(summary = "Refresh access token", tags = "Authentication")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token",
                    content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody final TokenRequest token) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.refresh(token));
    }

    @PostMapping(value = "/me/face-id", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enroll face ID for user", tags = "User Management")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Face ID enrolled successfully",
                content = @Content(schema = @Schema(implementation = SingleResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                content = @Content()),
        @ApiResponse(responseCode = "400", description = "Invalid request body",
                content = @Content()),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content()),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content())
    })
    public ResponseEntity<SingleResponse> enrollFaceId(@RequestParam("image") MultipartFile imageFile, @RequestAttribute("userId") String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.enrollFaceId(userId, imageFile));
    }

    @DeleteMapping("/me/face-id")
    @Operation(summary = "Delete face ID for user", tags = "User Management")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Face ID deleted successfully",
                content = @Content(schema = @Schema(implementation = SingleResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content()),
        @ApiResponse(responseCode = "404", description = "User not found",
                content = @Content()),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content())
    })
    public ResponseEntity<SingleResponse> deleteFaceId(@RequestAttribute("userId") String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.deleteFaceId(userId));
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update user account", tags = "User Management")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "409", description = "User account with provided email/phone already exists",
                    content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content()),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<UserResponse> updateUser(@Valid @ModelAttribute final UpdateUserRequest user, @RequestAttribute("userId") String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser(user, userId));
    }

    @GetMapping("/me")
    @Operation(summary = "Get user information", tags = "User Management")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<UserResponse> getUserInfo(@RequestAttribute("userId") String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserInfo(userId));
    }
    
    @PutMapping("/change-password")
    @Operation(summary = "Change user password", tags = "User Management")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content()),
            @ApiResponse(responseCode = "401", description = "Unauthorized or Current password is incorrect",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<SingleResponse> changePassword(@Valid @RequestBody final ChangePasswordRequest request, @RequestAttribute("userId") String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.changePassword(request, userId));
    }

    @GetMapping("/me/notifications")
    @Operation(summary = "Get user notifications", tags = "User Management")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User notifications retrieved successfully",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content()),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(@RequestAttribute("userId") String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserNotifications(userId));
    }

    @PostMapping("/me/mode-configs")
    @Operation(summary = "Create a new mode config", tags = "Mode Config Management")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Mode config created successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content()),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content()),
            @ApiResponse(responseCode = "409", description = "Mode config with the same name already exists",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<SingleResponse> createModeConfig(
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody CreateModeConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createModeConfig(userId, request));
    }

    @GetMapping("/me/mode-configs")
    @Operation(summary = "Get all mode configs of the user", tags = "Mode Config Management")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mode configs retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ModeConfigResponse[].class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<List<ModeConfigResponse>> getUserModeConfigs(@RequestAttribute("userId") String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUserModeConfigs(userId));
    }

    @GetMapping("/me/mode-configs/{modeId}")
    @Operation(summary = "Get a specific mode config", tags = "Mode Config Management")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mode config retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ModeConfigResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content()),
            @ApiResponse(responseCode = "404", description = "Mode config not found",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<ModeConfigResponse> getModeConfig(
            @RequestAttribute("userId") String userId,
            @PathVariable("modeId") String modeId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getModeConfig(userId, modeId));
    }

    @PutMapping("/me/mode-configs/{modeId}")
    @Operation(summary = "Update a mode config", tags = "Mode Config Management")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mode config updated successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content()),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content()),
            @ApiResponse(responseCode = "404", description = "Mode config not found",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<SingleResponse> updateModeConfig(
            @RequestAttribute("userId") String userId,
            @PathVariable("modeId") String modeId,
            @Valid @RequestBody UpdateModeConfigRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateModeConfig(userId, modeId, request));
    }

    @DeleteMapping("/me/mode-configs/{modeId}")
    @Operation(summary = "Delete a mode config", tags = "Mode Config Management")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Mode config deleted successfully",
                    content = @Content()),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content()),
            @ApiResponse(responseCode = "404", description = "Mode config not found",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<SingleResponse> deleteModeConfig(
            @RequestAttribute("userId") String userId,
            @PathVariable("modeId") String modeId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.deleteModeConfig(userId, modeId));
    }

    @PutMapping("/me/mode-configs/{modeId}/activate")
    @Operation(summary = "Activate a mode config", tags = "Mode Config Management")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mode config activated successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content()),
            @ApiResponse(responseCode = "404", description = "Mode config not found",
                    content = @Content()),
            @ApiResponse(responseCode = "409", description = "Mode config is already activated",
                    content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content())
    })
    public ResponseEntity<SingleResponse> activateModeConfig(
            @RequestAttribute("userId") String userId,
            @PathVariable("modeId") String modeId) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.activateModeConfig(userId, modeId));
    }
}
