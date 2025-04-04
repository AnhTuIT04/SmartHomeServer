package hcmut.smart_home.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;

import hcmut.smart_home.dto.SingleResponse;
import hcmut.smart_home.dto.sensor.FilterResponse;
import hcmut.smart_home.dto.sensor.PendingRequestResponse;
import hcmut.smart_home.dto.sensor.SensorInfoResponse;
import hcmut.smart_home.dto.sensor.UpdateSensorInfoRequest;
import hcmut.smart_home.dto.user.UserResponse;
import hcmut.smart_home.exception.BadRequestException;
import hcmut.smart_home.exception.ConflictException;
import hcmut.smart_home.exception.ForbiddenException;
import hcmut.smart_home.exception.InternalServerErrorException;
import hcmut.smart_home.exception.NotFoundException;

@Service
public class SensorService {

    private final Firestore firestore;

    public SensorService(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Retrieves sensor information for a given user.
     *
     * @param userId The ID of the user whose sensor information is to be retrieved.
     * @return A {@link SensorInfoResponse} object containing the sensor data.
     * @throws NotFoundException If the user does not exist, the user has no sensor assigned, 
     *                           or the sensor does not exist.
     * @throws InternalServerErrorException If an error occurs during the retrieval process.
     */
    public SensorInfoResponse getSensorInfo(String userId) {
        try {
            DocumentReference userDoc = firestore.collection("users").document(userId);
            DocumentSnapshot userSnapshot = userDoc.get().get();
            if (!userSnapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            String sensorId = userSnapshot.getString("sensorId");
            if (sensorId == null) {
                throw new NotFoundException("User has no sensor assigned");
            }

            DocumentReference sensorDoc = firestore.collection("sensors").document(sensorId);
            DocumentSnapshot sensorSnapshot = sensorDoc.get().get();
            if (!sensorSnapshot.exists()) {
                throw new NotFoundException("Sensor not found");
            }

            return new SensorInfoResponse(sensorSnapshot.getData());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Updates the sensor information for a given user based on the provided request.
     *
     * @param userId The ID of the user whose sensor information is to be updated.
     * @param request The request object containing the updated sensor information.
     * @return A {@link SensorInfoResponse} object containing the updated sensor data.
     * @throws NotFoundException If the user, their assigned sensor, or the sensor document does not exist.
     * @throws InternalServerErrorException If an error occurs during the update process.
     */
    public SensorInfoResponse updateSensorInfo(String userId, UpdateSensorInfoRequest request) {
        try {
            DocumentReference userDoc = firestore.collection("users").document(userId);
            DocumentSnapshot userSnapshot = userDoc.get().get();
            if (!userSnapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            String sensorId = userSnapshot.getString("sensorId");
            if (sensorId == null) {
                throw new NotFoundException("User has no sensor assigned");
            }

            DocumentReference sensorDoc = firestore.collection("sensors").document(sensorId);
            DocumentSnapshot sensorSnapshot = sensorDoc.get().get();
            if (!sensorSnapshot.exists()) {
                throw new NotFoundException("Sensor not found");
            }

            SensorInfoResponse sensorInfo = new SensorInfoResponse(sensorSnapshot.getData());
            WriteBatch batch = firestore.batch();
            boolean hasUpdates = false;
            
            if (request.getHumWarnUpper() != null) {
                batch.update(sensorDoc, "humWarnUpper", request.getHumWarnUpper());
                sensorInfo.setHumWarnUpper(request.getHumWarnUpper());
                hasUpdates = true;
            }

            if (request.getHumWarnLower() != null) {
                batch.update(sensorDoc, "humWarnLower", request.getHumWarnLower());
                sensorInfo.setHumWarnLower(request.getHumWarnLower());
                hasUpdates = true;
            }

            if (request.getTempWarnUpper() != null) {
                batch.update(sensorDoc, "tempWarnUpper", request.getTempWarnUpper());
                sensorInfo.setTempWarnUpper(request.getTempWarnUpper());
                hasUpdates = true;
            }

            if (request.getTempWarnLower() != null) {
                batch.update(sensorDoc, "tempWarnLower", request.getTempWarnLower());
                sensorInfo.setTempWarnLower(request.getTempWarnLower());
                hasUpdates = true;
            }

            if (request.getLightWarnUpper() != null) {
                batch.update(sensorDoc, "lightWarnUpper", request.getLightWarnUpper());
                sensorInfo.setLightWarnUpper(request.getLightWarnUpper());
                hasUpdates = true;
            }

            if (request.getLightWarnLower() != null) {
                batch.update(sensorDoc, "lightWarnLower", request.getLightWarnLower());
                sensorInfo.setLightWarnLower(request.getLightWarnLower());
                hasUpdates = true;
            }

            if (request.getHumForceUpper() != null) {
                batch.update(sensorDoc, "humForceUpper", request.getHumForceUpper());
                sensorInfo.setHumForceUpper(request.getHumForceUpper());
                hasUpdates = true;
            }

            if (request.getHumForceLower() != null) {
                batch.update(sensorDoc, "humForceLower", request.getHumForceLower());
                sensorInfo.setHumForceLower(request.getHumForceLower());
                hasUpdates = true;
            }

            if (request.getTempForceUpper() != null) {
                batch.update(sensorDoc, "tempForceUpper", request.getTempForceUpper());
                sensorInfo.setTempForceUpper(request.getTempForceUpper());
                hasUpdates = true;
            }

            if (request.getTempForceLower() != null) {
                batch.update(sensorDoc, "tempForceLower", request.getTempForceLower());
                sensorInfo.setTempForceLower(request.getTempForceLower());
                hasUpdates = true;
            }

            if (request.getLightForceUpper() != null) {
                batch.update(sensorDoc, "lightForceUpper", request.getLightForceUpper());
                sensorInfo.setLightForceUpper(request.getLightForceUpper());
                hasUpdates = true;
            }

            if (request.getLightForceLower() != null) {
                batch.update(sensorDoc, "lightForceLower", request.getLightForceLower());
                sensorInfo.setLightForceLower(request.getLightForceLower());
                hasUpdates = true;
            }

            if (hasUpdates) {
                batch.commit().get();
            }

            return sensorInfo;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Subscribes a user to a sensor. If the sensor does not exist, it will be created and assigned to the user.
     * If the sensor exists and is not assigned to any user, it will be assigned to the requesting user.
     * If the sensor is already assigned to another user, a subscription request will be created or removed.
     *
     * @param sensorId the ID of the sensor to subscribe to
     * @param userId the ID of the user subscribing to the sensor
     * @return a {@link SingleResponse} indicating the result of the subscription operation
     * @throws NotFoundException if the user does not exist
     * @throws ConflictException if the user already has a sensor assigned
     * @throws InternalServerErrorException if an internal server error occurs
     */
    public SingleResponse subscribe(String sensorId, String userId) {
        try {
            // References to Firestore documents and collections
            DocumentReference sensorDoc = firestore.collection("sensors").document(sensorId);
            DocumentReference userDoc = firestore.collection("users").document(userId);
            CollectionReference requestsCollection = firestore.collection("requests");
    
            // Run Firestore queries in parallel for better performance
            ApiFuture<DocumentSnapshot> userFuture = userDoc.get();
            ApiFuture<DocumentSnapshot> sensorFuture = sensorDoc.get();
    
            DocumentSnapshot userSnapshot = userFuture.get();
            DocumentSnapshot sensorSnapshot = sensorFuture.get();
    
            // Check if the user exists
            if (!userSnapshot.exists()) {
                throw new NotFoundException("User not found");
            }
    
            // Check if the user already has a sensor assigned
            String existingSensorId = userSnapshot.getString("sensorId");
            if (existingSensorId != null) {
                throw new ConflictException("User already has a sensor assigned.");
            }
    
            // If the sensor exists
            if (sensorSnapshot.exists()) {
                String ownerId = sensorSnapshot.getString("ownerId");
    
                // If the user is already the owner, do nothing
                if (userId.equals(ownerId)) {
                    return new SingleResponse("Sensor already assigned to user.");
                }
    
                // If the sensor is unassigned, assign it to the user
                if (ownerId == null) {
                    WriteBatch batch = firestore.batch();
                    SensorInfoResponse sensorInfo = new SensorInfoResponse(sensorId, userId);
                    batch.update(sensorDoc, sensorInfo.toMap());
                    batch.update(userDoc, "sensorId", sensorId);
    
                    // Remove any existing subscription requests for this user
                    QuerySnapshot requestSnapshot = requestsCollection.whereEqualTo("userId", userId).get().get();
                    requestSnapshot.getDocuments().forEach(doc -> batch.delete(doc.getReference()));
    
                    batch.commit().get(); // Execute batch operations
    
                    return new SingleResponse("Sensor assigned to user.");
                }
    
                // If the sensor already has an owner, check for an existing subscription request
                QuerySnapshot requestSnapshot = requestsCollection
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("sensorId", sensorId)
                        .get()
                        .get();
    
                if (!requestSnapshot.isEmpty()) {
                    // If a request already exists, remove it (toggle behavior)
                    WriteBatch batch = firestore.batch();
                    requestSnapshot.getDocuments().forEach(doc -> batch.delete(doc.getReference()));
                    batch.commit().get();
                    return new SingleResponse("Subscription request removed.");
                }
    
                // Create a new subscription request
                requestsCollection.add(Map.of(
                    "userId", userId,
                    "sensorId", sensorId,
                    "createdAt", Timestamp.now()
                ));
    
                return new SingleResponse("Request to subscribe sent.");
            }
    
            // If the sensor does not exist, create it and assign it to the user
            WriteBatch batch = firestore.batch();
            SensorInfoResponse sensorInfo = new SensorInfoResponse(sensorId, userId);
            batch.set(sensorDoc, sensorInfo.toMap());
            batch.update(userDoc, "sensorId", sensorId);
    
            // Remove any existing subscription requests for this user
            QuerySnapshot requestSnapshot = requestsCollection.whereEqualTo("userId", userId).get().get();
            requestSnapshot.getDocuments().forEach(doc -> batch.delete(doc.getReference()));
    
            batch.commit().get(); // Execute batch operations
    
            return new SingleResponse("Sensor created and assigned to user.");
    
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }    

    /**
     * Unsubscribes a user from a sensor or unassigns a sensor from its owner and removes all subscribers.
     *
     * @param userId The ID of the user to unsubscribe.
     * @return A {@link SingleResponse} indicating the result of the operation.
     * @throws NotFoundException If the user or sensor does not exist.
     * @throws BadRequestException If the user has not subscribed to any sensor.
     * @throws InternalServerErrorException If an error occurs during the operation.
     *
     * This method performs the following steps:
     * 1. Validates the existence of the user by checking the Firestore document.
     * 2. Checks if the user is subscribed to a sensor. If not, throws a BadRequestException.
     * 3. Validates the existence of the sensor associated with the user.
     * 4. If the user is not the owner of the sensor, unsubscribes the user by removing the sensorId from the user's document.
     * 5. If the user is the owner of the sensor:
     *    - Unassigns the sensor from the owner by setting the ownerId to null.
     *    - Removes all subscribers by clearing the sensorId field from all subscribed users.
     *    - Deletes all subscription requests associated with the sensor.
     * 6. Ensures all updates are committed to Firestore.
     */
    public SingleResponse unsubscribe(String userId) {
        try {
            DocumentReference userDoc = firestore.collection("users").document(userId);
            ApiFuture<DocumentSnapshot> userFuture = userDoc.get();
            DocumentSnapshot userSnapshot = userFuture.get();

            // Validate user existence
            if (!userSnapshot.exists()) {
                throw new NotFoundException("User not Found");
            }

            String sensorId = userSnapshot.getString("sensorId");
            if (sensorId == null) {
                throw new BadRequestException("User has not subscribed to any sensor.");
            }
            
            DocumentReference sensorDoc = firestore.collection("sensors").document(sensorId);
            ApiFuture<DocumentSnapshot> sensorFuture = sensorDoc.get();
            DocumentSnapshot sensorSnapshot = sensorFuture.get();


            if (!sensorSnapshot.exists()) {
                throw new NotFoundException("Sensor not found.");
            }

            if (!userId.equals(sensorSnapshot.getString("ownerId"))) {
                // Unsubscribe user from sensor
                userDoc.update("sensorId", null);
                return new SingleResponse("User unsubscribed from sensor.");
            } 

            // Unassign sensor from user and remove all subscribers
            ApiFuture<WriteResult> sensorUpdateFuture = sensorDoc.update("ownerId", null, "updatedAt", Timestamp.now());

            // Query all users subscribed to sensor
            ApiFuture<QuerySnapshot> usersFuture = firestore.collection("users")
                .whereEqualTo("sensorId", sensorId)
                .get();

            // Query all requests to subscribe to sensor
            ApiFuture<QuerySnapshot> requestsFuture = firestore.collection("requests")
                .whereEqualTo("sensorId", sensorId)
                .get();

            // Wait for all queries to complete
            QuerySnapshot usersSnapshot = usersFuture.get();
            QuerySnapshot requestsSnapshot = requestsFuture.get();

            // If there are users or requests, create a batch to update all in a single transaction
            if (!usersSnapshot.isEmpty() || !requestsSnapshot.isEmpty()) {
                WriteBatch batch = firestore.batch();

                // Delete sensorId from all users subscribed to sensor
                for (DocumentSnapshot doc : usersSnapshot.getDocuments()) {
                    batch.update(doc.getReference(), "sensorId", null);
                }

                // Delete all requests to subscribe to sensor
                for (DocumentSnapshot doc : requestsSnapshot.getDocuments()) {
                    batch.delete(doc.getReference());
                }

                // Commit batch
                batch.commit().get();
            }

            // Wait for sensor update to complete
            sensorUpdateFuture.get();

            return new SingleResponse("Sensor unassigned and subscribers removed.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Removes a user's access to a sensor owned by another user.
     *
     * @param ownerId The ID of the owner of the sensor.
     * @param userId The ID of the user whose access is to be removed.
     * @return A {@link SingleResponse} object containing a success message if the operation is successful.
     * @throws NotFoundException If the owner or user does not exist, or if the sensor does not exist.
     * @throws BadRequestException If the owner or user is not subscribed to any sensor, or if the user is not subscribed to the owner's sensor.
     * @throws ForbiddenException If the owner does not have permission to remove the user's access, or if the owner attempts to remove their own access.
     * @throws InternalServerErrorException If an unexpected error occurs during the operation.
     */
    public SingleResponse removeUserAccess(String ownerId, String userId) {
        try {
            CollectionReference usersCollection = firestore.collection("users");
            CollectionReference sensorsCollection = firestore.collection("sensors");

            // Check if users exists
            DocumentSnapshot ownerSnapshot = usersCollection.document(ownerId).get().get();
            DocumentSnapshot userSnapshot = usersCollection.document(userId).get().get();
            if (!ownerSnapshot.exists() || !userSnapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Get sensorId of users
            String ownerSensorId = ownerSnapshot.getString("sensorId");
            String userSensorId = userSnapshot.getString("sensorId");
            if (ownerSensorId == null || userSensorId == null) {
                throw new BadRequestException("User not subscribed to any sensor");
            }

            // Get sensor document
            DocumentSnapshot sensorSnapshot = sensorsCollection.document(ownerSensorId).get().get();
            if (!sensorSnapshot.exists()) {
                throw new NotFoundException("Sensor not found");
            }

            // Check if user is the owner of the sensor
            if (!ownerId.equals(sensorSnapshot.getString("ownerId"))) {
                throw new ForbiddenException("User does not have permission to remove user access");
            }

            // Check if user is subscribed to the sensor
            if (!userSensorId.equals(ownerSensorId)) {
                throw new BadRequestException("User is not subscribed to your sensor");
            }

            // Check if user is the owner of the sensor
            if (ownerId.equals(userId)) {
                throw new ForbiddenException("Owner cannot remove their own access");
            }

            // Remove sensorId from user
            usersCollection.document(userId).update("sensorId", null);

            return new SingleResponse("User access removed successfully");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Retrieves a list of subscribers for a given user based on the sensor they own.
     *
     * @param userId The ID of the user requesting the subscriber list.
     * @return A list of {@link UserResponse} objects representing the subscribers.
     * @throws NotFoundException If the user or the sensor does not exist.
     * @throws ForbiddenException If the user does not own the sensor or does not have permission to view subscribers.
     * @throws InternalServerErrorException If an error occurs during the execution of the Firestore operations.
     */
    public List<UserResponse> getSubscribers(String userId) {
        try {
            CollectionReference usersCollection = firestore.collection("users");
            CollectionReference sensorsCollection = firestore.collection("sensors");

            // Check if user exists
            DocumentSnapshot userSnapshot = usersCollection.document(userId).get().get();
            if (!userSnapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            String sensorId = userSnapshot.getString("sensorId");
            if (sensorId == null) {
                throw new ForbiddenException("User does not have permission to view subscribers");
            }

            // Check if sensor exists
            DocumentSnapshot sensorSnapshot = sensorsCollection.document(sensorId).get().get();
            if (!sensorSnapshot.exists()) {
                throw new NotFoundException("Sensor not found");
            }

            // Check if user is the owner of the sensor
            String ownerId = sensorSnapshot.getString("ownerId");
            if (ownerId == null || !ownerId.equals(userId)) {
                throw new ForbiddenException("User does not have permission to view subscribers");
            }

            // Query all users subscribed to sensor
            QuerySnapshot userQuerySnapshot = usersCollection.whereEqualTo("sensorId", sensorId).get().get();

            // Map query results to response DTO
            return userQuerySnapshot.getDocuments().stream()
                .map(doc -> new UserResponse(
                    doc.getId(),
                    doc.getString("firstName"),
                    doc.getString("lastName"),
                    doc.getString("email"),
                    doc.getString("phone"),
                    doc.getString("avatar"),
                    doc.getString("sensorId")
                ))
                .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Retrieves a list of pending subscription requests for a sensor associated with a user.
     *
     * @param userId The ID of the user requesting the pending subscription requests.
     * @return A list of {@link PendingRequestResponse} objects representing the pending requests.
     * @throws NotFoundException If the user or the sensor associated with the user is not found.
     * @throws ForbiddenException If the user does not have permission to view the requests.
     * @throws InternalServerErrorException If an error occurs during the execution of the request.
     */
    public List<PendingRequestResponse> getRequests(String userId) {
        try {
            CollectionReference usersCollection = firestore.collection("users");
            CollectionReference sensorsCollection = firestore.collection("sensors");
            CollectionReference requestsCollection = firestore.collection("requests");

            // Check if user exists
            DocumentSnapshot userSnapshot = usersCollection.document(userId).get().get();
            if (!userSnapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            String sensorId = userSnapshot.getString("sensorId");
            if (sensorId == null) {
                throw new ForbiddenException("User does not have permission to view requests");
            }

            // Check if sensor exists
            DocumentSnapshot sensorSnapshot = sensorsCollection.document(sensorId).get().get();
            if (!sensorSnapshot.exists()) {
                throw new NotFoundException("Sensor not found");
            }

            // Check if user is the owner of the sensor
            String ownerId = sensorSnapshot.getString("ownerId");
            if (ownerId == null || !ownerId.equals(userId)) {
                throw new ForbiddenException("User does not have permission to view requests");
            }

            // Query all requests to subscribe to sensor
            QuerySnapshot requestSnapshot = requestsCollection
                .whereEqualTo("sensorId", sensorId)
                .get()
                .get();

            // Map query results to response DTO
            return requestSnapshot.getDocuments().stream()
                .map(doc -> new PendingRequestResponse(
                    doc.getId(),
                    doc.getString("sensorId"),
                    doc.getString("userId"),
                    doc.getTimestamp("createdAt")
                ))
                .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Approves a request for a user to subscribe to a sensor.
     *
     * @param userId    The ID of the user who is approving the request.
     * @param requestId The ID of the request to be approved.
     * @return A {@link SingleResponse} object containing a success message if the request is approved successfully.
     * @throws NotFoundException         If the user, request, or sensor is not found.
     * @throws ForbiddenException        If the user is not the owner of the sensor.
     * @throws InternalServerErrorException If an error occurs during execution.
     */
    public SingleResponse approveRequest(String userId, String requestId) {
        try {
            CollectionReference usersCollection = firestore.collection("users");
            CollectionReference requestsCollection = firestore.collection("requests");

            // Check if user exists
            DocumentSnapshot userSnapshot = usersCollection.document(userId).get().get();
            if (!userSnapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Check if request exists
            DocumentSnapshot requestSnapshot = requestsCollection.document(requestId).get().get();
            if (!requestSnapshot.exists()) {
                throw new NotFoundException("Request not found");
            }

            // Check if user is the owner of the sensor
            String sensorId = requestSnapshot.getString("sensorId");
            if (sensorId == null) {
                throw new NotFoundException("Sensor not found");
            }

            DocumentSnapshot sensorSnapshot = firestore.collection("sensors").document(sensorId).get().get();
            if (!userId.equals(sensorSnapshot.getString("ownerId"))) {
                throw new ForbiddenException("User is not the owner of the sensor");
            }

            // Update user document to subscribe to sensor
            String requesterId = requestSnapshot.getString("userId");
            if (requesterId == null) {
                throw new NotFoundException("User not found");
            }
            usersCollection.document(requesterId).update("sensorId", sensorId);

            // Delete request document
            requestsCollection.document(requestId).delete();

            return new SingleResponse("Request approved successfully");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Rejects a request associated with a sensor.
     *
     * @param userId    The ID of the user attempting to reject the request.
     * @param requestId The ID of the request to be rejected.
     * @return A {@link SingleResponse} object containing a success message if the request is rejected successfully.
     * @throws NotFoundException        If the user, request, or sensor does not exist.
     * @throws ForbiddenException       If the user is not the owner of the sensor associated with the request.
     * @throws InternalServerErrorException If an error occurs during the operation.
     */
    public SingleResponse rejectRequest(String userId, String requestId) {
        try {
            CollectionReference usersCollection = firestore.collection("users");
            CollectionReference requestsCollection = firestore.collection("requests");

            // Check if user exists
            DocumentSnapshot userSnapshot = usersCollection.document(userId).get().get();
            if (!userSnapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Check if request exists
            DocumentSnapshot requestSnapshot = requestsCollection.document(requestId).get().get();
            if (!requestSnapshot.exists()) {
                throw new NotFoundException("Request not found");
            }

            // Check if user is the owner of the sensor
            String sensorId = requestSnapshot.getString("sensorId");
            if (sensorId == null) {
                throw new NotFoundException("Sensor not found");
            }

            DocumentSnapshot sensorSnapshot = firestore.collection("sensors").document(sensorId).get().get();
            if (!userId.equals(sensorSnapshot.getString("ownerId"))) {
                throw new ForbiddenException("User is not the owner of the sensor");
            }

            // Delete request document
            requestsCollection.document(requestId).delete();

            return new SingleResponse("Request rejected successfully");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Retrieves a list of chart filters based on the specified parameters.
     *
     * @param userId    The ID of the user requesting the chart filters.
     * @param field     The field to filter by. Must be one of "humidity", "light_intensity", or "temperature".
     * @param min       The minimum value for the specified field (optional).
     * @param max       The maximum value for the specified field (optional).
     * @param startTime The start time for the filter in epoch seconds (optional, defaults to 3 months ago if null).
     * @param endTime   The end time for the filter in epoch seconds (optional, defaults to the current time if null).
     * @return A list of {@link FilterResponse} objects containing the filtered data.
     * @throws BadRequestException        If the field is null or not one of the allowed values.
     * @throws NotFoundException          If the user or sensor is not found.
     * @throws ForbiddenException         If the user does not have permission to access the sensor.
     * @throws InternalServerErrorException If an error occurs during query execution.
     */
    public List<FilterResponse> getChartFilters(String userId, String field, Double min, Double max, Long startTime, Long endTime) {
        try {
            if (field == null || !List.of("humidity", "light_intensity", "temperature").contains(field)) {
                throw new BadRequestException("Field must be 'humidity', 'light_intensity' or 'temperature'");
            }

            CollectionReference usersCollection = firestore.collection("users");
            CollectionReference sensorsCollection = firestore.collection("sensors");

            // Check if user exists
            DocumentSnapshot userSnapshot = usersCollection.document(userId).get().get();
            if (!userSnapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            String sensorId = userSnapshot.getString("sensorId");
            if (sensorId == null) {
                throw new ForbiddenException("User does not have permission to get chart filters");
            }

            // Check if sensor exists
            DocumentSnapshot sensorSnapshot = sensorsCollection.document(sensorId).get().get();
            if (!sensorSnapshot.exists()) {
                throw new NotFoundException("Sensor not found");
            }

            // Default start time is 3 months ago
            if (startTime == null) {
                startTime = Instant.now().minus(90, ChronoUnit.DAYS).getEpochSecond();
            }
            // Default end time is now
            if (endTime == null) {
                endTime = Instant.now().getEpochSecond();  
            }

            Query query = firestore.collection("user_sensor").whereEqualTo("sensorId", sensorId);

            if (min != null) query = query.whereGreaterThanOrEqualTo(field, min);
            if (max != null) query = query.whereLessThanOrEqualTo(field, max);

            query = query.whereGreaterThanOrEqualTo("timestamp", startTime)
                        .whereLessThanOrEqualTo("timestamp", endTime)
                        .orderBy("timestamp"); 

            ApiFuture<QuerySnapshot> future = query.get();

            return future.get().getDocuments().stream()
                .map(doc -> new FilterResponse(
                    doc.getId(),
                    Objects.requireNonNullElse(doc.getDouble(field), 0.0),
                    Objects.requireNonNullElse(doc.getLong("timestamp"), 0L) 
                ))
                .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

}
