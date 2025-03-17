package hcmut.smart_home.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hcmut.smart_home.config.PublicEndpoint;
import hcmut.smart_home.dto.user.AuthResponse;
import hcmut.smart_home.dto.user.CreateUserRequest;
import hcmut.smart_home.dto.user.LoginUserRequest;
import hcmut.smart_home.dto.user.TokenRequest;
import hcmut.smart_home.dto.user.TokenResponse;
import hcmut.smart_home.dto.user.UpdateUserRequest;
import hcmut.smart_home.dto.user.UserResponse;
import hcmut.smart_home.service.UserService;
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
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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
    @PostMapping("/login")
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
    @PostMapping("/refresh")
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

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    
    @PutMapping("/change-password")
    public String changePassword(@PathVariable String id, @RequestBody String entity) {
        //TODO: process PUT request
        
        return entity;
    }
}
