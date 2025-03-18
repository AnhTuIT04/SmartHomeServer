package hcmut.smart_home.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hcmut.smart_home.dto.SingleResponse;
import hcmut.smart_home.dto.sensor.PendingRequestResponse;
import hcmut.smart_home.dto.user.UserResponse;
import hcmut.smart_home.service.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/sensor")
public class SensorController {

    private final SensorService sensorService;
    
    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @PostMapping("/{sensorId}/user/subscribe")
    @Operation(summary = "Subscribe to a sensor, if sensor is not assigned to any user, sensor will be assigned to the subscriber", tags = "Sensor")
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Subscribed successfully",
            content = @Content(schema = @Schema(implementation = SingleResponse.class))),
        @ApiResponse(responseCode = "404", description = "Sensor/User not found",
            content = @Content()),
        @ApiResponse(responseCode = "409", description = "Sensor already assigned to another user",
            content = @Content()),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content())
    })
    public ResponseEntity<SingleResponse> subscribe(@PathVariable String sensorId, @RequestAttribute("userId") String userId) {
        return ResponseEntity.ok().body(sensorService.subscribe(sensorId, userId));
    }
    
    @PutMapping("/user/unsubscribe")
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Unsubscribed successfully",
            content = @Content(schema = @Schema(implementation = SingleResponse.class))),
        @ApiResponse(responseCode = "404", description = "Sensor/User not found",
            content = @Content()),
        @ApiResponse(responseCode = "400", description = "User has not subscribed to any sensor",
            content = @Content()),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content())
    })
    @Operation(summary = "Unsubscribe from a sensor, if subscriber is the owner, sensor will be unassigned and all subscribers will be removed", tags = "Sensor")
    public ResponseEntity<SingleResponse> unsubscribe( @RequestAttribute("userId") String userId) {
        return ResponseEntity.ok().body(sensorService.unsubscribe(userId));
    }
    
    @GetMapping("/user/subscribers")
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Get subscribers successfully",
            content = @Content(schema = @Schema(implementation = PendingRequestResponse.class))),
        @ApiResponse(responseCode = "404", description = "Sensor/User not found",
            content = @Content()),
        @ApiResponse(responseCode = "403", description = "User is not the owner of the sensor",
            content = @Content()),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content())
    })
    @Operation(summary = "Get subscribers of a sensor", tags = "Sensor")
    public ResponseEntity<List<UserResponse>> getSubscribers(@RequestAttribute("userId") String userId) {
        return ResponseEntity.ok().body(sensorService.getSubscribers(userId));
    }

    @GetMapping("/user/requests")
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Requests retrieved successfully",
            content = @Content(schema = @Schema(implementation = PendingRequestResponse.class))),
        @ApiResponse(responseCode = "404", description = "Sensor/User not found",
            content = @Content()),
        @ApiResponse(responseCode = "403", description = "User is not the owner of the sensor",
            content = @Content()),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content())
    })
    @Operation(summary = "Get pending requests to subscribe to a sensor", tags = "Sensor")
    public ResponseEntity<List<PendingRequestResponse>> getRequests(@RequestAttribute("userId") String userId) {
        return ResponseEntity.ok().body(sensorService.getRequests(userId));
    }

    @PostMapping("/requests/{requestId}/approve")
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Request approved successfully",
            content = @Content(schema = @Schema(implementation = SingleResponse.class))),
        @ApiResponse(responseCode = "403", description = "User is not the owner of the sensor",
            content = @Content()),
        @ApiResponse(responseCode = "404", description = "Request not found",
            content = @Content()),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content())
    })
    @Operation(summary = "Approve a request to subscribe to a sensor", tags = "Sensor")
    public ResponseEntity<SingleResponse> approveRequest(@RequestAttribute("userId") String userId, @PathVariable String requestId) {
        return ResponseEntity.ok().body(sensorService.approveRequest(userId, requestId));
    }

    @PostMapping("/requests/{requestId}/reject")
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Request rejected successfully",
            content = @Content(schema = @Schema(implementation = SingleResponse.class))),
        @ApiResponse(responseCode = "403", description = "User is not the owner of the sensor",
            content = @Content()),
        @ApiResponse(responseCode = "404", description = "Request not found",
            content = @Content()),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content())
    })
    @Operation(summary = "Reject a request to subscribe to a sensor", tags = "Sensor")
    public ResponseEntity<SingleResponse> rejectRequest(@RequestAttribute("userId") String userId, @PathVariable String requestId) {
        return ResponseEntity.ok().body(sensorService.rejectRequest(userId, requestId));
    }
}