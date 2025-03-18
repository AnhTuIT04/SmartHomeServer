package hcmut.smart_home.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hcmut.smart_home.config.PublicEndpoint;
import hcmut.smart_home.dto.SingleResponse;
import hcmut.smart_home.dto.user.AuthResponse;
import hcmut.smart_home.dto.user.ChangePasswordRequest;
import hcmut.smart_home.dto.user.CreateUserRequest;
import hcmut.smart_home.dto.user.LoginUserRequest;
import hcmut.smart_home.dto.user.TokenRequest;
import hcmut.smart_home.dto.user.TokenResponse;
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
}
