package hcmut.smart_home.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;

import hcmut.smart_home.dto.FaceEmbedding.FaceEmbedding;
import hcmut.smart_home.dto.PaginationResponse;
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
import hcmut.smart_home.exception.BadRequestException;
import hcmut.smart_home.exception.ConflictException;
import hcmut.smart_home.exception.ForbiddenException;
import hcmut.smart_home.exception.InternalServerErrorException;
import hcmut.smart_home.exception.NotFoundException;
import hcmut.smart_home.exception.UnauthorizedException;
import hcmut.smart_home.util.Argon;
import hcmut.smart_home.util.CloudinaryUtil;
import hcmut.smart_home.util.Jwt;

@Service
public class UserService {

    private final Firestore firestore;
    private final Jwt jwt;
    private final CloudinaryUtil cloudinaryUtil;
    private final NotificationService notificationService;
    private final SensorDataService sensorDataService;
    private final FaceEmbeddingService faceEmbeddingService;

    public UserService(Firestore firestore, Jwt jwt, CloudinaryUtil cloudinaryUtil, NotificationService notificationService, SensorDataService sensorDataService, FaceEmbeddingService faceEmbeddingService) {
        this.firestore = firestore;
        this.jwt = jwt;
        this.cloudinaryUtil = cloudinaryUtil;
        this.notificationService = notificationService;
        this.sensorDataService = sensorDataService;
        this.faceEmbeddingService = faceEmbeddingService;
    }

    /**
     * Creates a new user in the Firestore database.
     *
     * @param user The request object containing user details.
     * @return UserResponse containing user details and authentication tokens.
     * @throws ConflictException if the email or phone number already exists.
     * @throws InternalServerErrorException if there is an issue interacting with Firestore.
     */
    public AuthResponse createUser(final CreateUserRequest user) {
        try {
            CollectionReference usersCollection = firestore.collection("users");

            // Validate if email or phone already exists
            if (isEmailTaken(usersCollection, user.getEmail(), null)) {
                throw new ConflictException("Email already exists");
            }

            if (isPhoneTaken(usersCollection, user.getPhone(), null)) {
                throw new ConflictException("Phone number already exists");
            }

            // Create new document reference for the user
            DocumentReference docRef = usersCollection.document();
            String userId = docRef.getId();

            // Hash password before storing
            String hashedPassword = Argon.hashPassword(user.getPassword());
            user.setPassword(hashedPassword);

            // Store user data in Firestore
            docRef.set(user).get();  

            // Generate authentication tokens
            String accessToken = jwt.generateAccessToken(userId);
            String refreshToken = jwt.generateRefreshToken(userId);

            // Return the created user response
            return new AuthResponse(user, docRef.getId(), accessToken, refreshToken);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Authenticates a user using email and password.
     *
     * @param user The login request containing email and password.
     * @return UserResponse containing user details and authentication tokens.
     * @throws UnauthorizedException if the email or password is incorrect.
     * @throws InternalServerErrorException if there is an issue fetching user data from Firestore.
     */
    public AuthResponse login(LoginUserRequest user) {
        try {
            // Reference to the "users" collection
            CollectionReference usersCollection = firestore.collection("users");

            // Query Firestore for user with the given email
            Query emailQuery = usersCollection.whereEqualTo("email", user.getEmail());
            ApiFuture<QuerySnapshot> emailQuerySnapshot = emailQuery.get();
            List<QueryDocumentSnapshot> documents = emailQuerySnapshot.get().getDocuments();

            // Validate user existence
            if (documents.isEmpty()) {
                throw new UnauthorizedException("Invalid email or password");
            }

            // Retrieve the user document
            DocumentSnapshot userDoc = documents.get(0);
            String storedPassword = userDoc.getString("password");

            // Validate password
            if (!Argon.compare(user.getPassword(), storedPassword)) {
                throw new UnauthorizedException("Invalid email or password");
            }

            // Extract user details
            String userId = userDoc.getId();
            String firstName = userDoc.getString("firstName");
            String lastName = userDoc.getString("lastName");
            String phone = userDoc.getString("phone");
            String avatar = userDoc.getString("avatar");
            String sensorId = userDoc.getString("sensorId");
            Boolean enrolledFaceIdObj = userDoc.getBoolean("isEnrolledFaceId");
            boolean isEnrolledFaceId = enrolledFaceIdObj != null && enrolledFaceIdObj;

            // Generate authentication tokens
            String accessToken = jwt.generateAccessToken(userId);
            String refreshToken = jwt.generateRefreshToken(userId);

            // Return the user response
            return new AuthResponse(user, userId, firstName, lastName, phone, avatar, sensorId, isEnrolledFaceId, accessToken, refreshToken);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }
    
    /**
     * Authenticates a user using Face ID embedding.
     * <p>
     * This method compares the provided face embedding with all stored embeddings in the "face-ids" Firestore collection.
     * If a match is found within a specified threshold, the corresponding user is authenticated and JWT tokens are generated.
     * </p>
     *
     * @param faceId The {@link FaceEmbedding} containing the user's face embedding to authenticate.
     * @return {@link AuthResponse} containing user information and authentication tokens if authentication is successful.
     * @throws UnauthorizedException If no matching face embedding is found or the user does not exist.
     * @throws InternalServerErrorException If an internal error occurs during authentication.
     */
    public AuthResponse loginWithFaceId(MultipartFile image) {
        try {
            // Get the face embedding from the provided image
            FaceEmbedding faceId = faceEmbeddingService.getEmbedding(image);
            List<Double> inputEmbedded = faceId.getEmbedding();

            // Get all face IDs from Firestore
            ApiFuture<QuerySnapshot> future = firestore.collection("face-ids").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            String matchedUserId = null;
            double maxSimilarity = 0.0;

            for (QueryDocumentSnapshot doc : documents) {
                List<?> embeddingRaw = (List<?>) doc.get("embedding");
                if (embeddingRaw == null || embeddingRaw.size() != inputEmbedded.size()) continue;

                List<Double> storedEmbedding = embeddingRaw.stream()
                        .map(o -> o instanceof Number ? ((Number) o).doubleValue() : null)
                        .toList();

                FaceEmbedding storedFace = new FaceEmbedding();
                storedFace.setEmbedding(storedEmbedding);

                double similarity = faceEmbeddingService.calculateSimilarity(inputEmbedded, storedFace.getEmbedding());
                if (similarity > maxSimilarity && similarity >= faceEmbeddingService.getThreshold()) {
                    maxSimilarity = similarity;
                    matchedUserId = doc.getId();
                }
            }

            if (matchedUserId == null) {
                throw new UnauthorizedException("Face ID verification failed" + String.format(" maxSimilarity: %.2f", maxSimilarity));
            }

            // Check if the user exists in the "users" collection
            DocumentSnapshot userDoc = firestore.collection("users").document(matchedUserId).get().get();
            if (!userDoc.exists()) {
                throw new UnauthorizedException("Face ID verification failed" + String.format(" maxSimilarity: %.2f", maxSimilarity));
            }

            // Generate authentication tokens
            String accessToken = jwt.generateAccessToken(matchedUserId);
            String refreshToken = jwt.generateRefreshToken(matchedUserId);

            // Extract user details
            String firstName = userDoc.getString("firstName");
            String lastName = userDoc.getString("lastName");
            String email = userDoc.getString("email");
            String phone = userDoc.getString("phone");
            String avatar = userDoc.getString("avatar");
            String sensorId = userDoc.getString("sensorId");
            Boolean isEnrolledFaceIdObj = userDoc.getBoolean("isEnrolledFaceId");
            boolean isEnrolledFaceId = isEnrolledFaceIdObj != null && isEnrolledFaceIdObj;

            // Return the user response
            return new AuthResponse(matchedUserId, firstName, lastName, email, phone, avatar, sensorId, isEnrolledFaceId, accessToken, refreshToken);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Refreshes the access token using the provided refresh token.
     *
     * @param token the token request containing the refresh token
     * @return a TokenResponse containing the new access token
     * @throws UnauthorizedException if the refresh token is invalid or expired, or if token refresh fails
     */
    public TokenResponse refresh(TokenRequest token) {
        // Extract refresh token from the request
        String refreshToken = token.getRefreshToken();

        // Validate refresh token
        if (!jwt.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        try {
            // Extract user ID from the refresh token
            String userId = jwt.extractId(refreshToken);

            // Generate and return a new access token
            return new TokenResponse(jwt.generateAccessToken(userId), jwt.generateAccessToken(userId));
        } catch (Exception e) {
            throw new UnauthorizedException("Failed to refresh token");
        }
    }
    
    /**
     * Enrolls a face ID for a user by extracting a face embedding from the provided image file
     * and storing it in the Firestore database under the 'face-ids' collection with the user's ID.
     *
     * @param userId     The unique identifier of the user to enroll the face ID for.
     * @param imageFile  The image file containing the user's face to be processed.
     * @return           A {@link SingleResponse} indicating the result of the enrollment operation.
     * @throws NotFoundException           If the user with the specified ID does not exist.
     * @throws InternalServerErrorException If an error occurs during Firestore operations or face embedding extraction.
     */
    public SingleResponse enrollFaceId(String userId, MultipartFile imageFile) {
        try {
            // Get reference to the user document in Firestore
            CollectionReference usersCollection = firestore.collection("users");
            DocumentReference docRef = usersCollection.document(userId);

            // Check if the user exists
            var snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Save faceId to 'face-ids' collection with document ID = userId
            CollectionReference faceIdCollection = firestore.collection("face-ids");
            DocumentReference faceIdDocRef = faceIdCollection.document(userId);

            // Extract face embedding from the image file
            FaceEmbedding faceId = faceEmbeddingService.getEmbedding(imageFile);

            // Use batch to write face embedding and update user enrollment status atomically
            WriteBatch batch = firestore.batch();
            batch.set(faceIdDocRef, faceId);
            batch.update(docRef, "isEnrolledFaceId", true);

            // Commit the batch operation
            batch.commit().get();

            return new SingleResponse("Face ID enrolled successfully");

        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        }
    }

    /**
     * Deletes the Face ID associated with the specified user.
     * <p>
     * This method performs the following steps:
     * <ul>
     *   <li>Checks if the user with the given {@code userId} exists in the Firestore database.</li>
     *   <li>Checks if a Face ID document exists for the user.</li>
     *   <li>If both exist, deletes the Face ID document and updates the user's {@code isEnrolledFaceId} status to {@code false} in a batch operation.</li>
     * </ul>
     * If the user or Face ID does not exist, a {@link NotFoundException} is thrown.
     * If an internal error occurs during the operation, an {@link InternalServerErrorException} is thrown.
     *
     * @param userId the unique identifier of the user whose Face ID is to be deleted
     * @return a {@link SingleResponse} indicating the result of the operation
     * @throws NotFoundException if the user or Face ID does not exist
     * @throws InternalServerErrorException if an error occurs during the deletion process
     */
    public SingleResponse deleteFaceId(String userId) {
        try {
            // Check if the user exists
            DocumentReference userDocRef = firestore.collection("users").document(userId);
            var userSnapshot = userDocRef.get().get();
            if (!userSnapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Get reference to the face ID document in Firestore
            DocumentReference faceIdDocRef = firestore.collection("face-ids").document(userId);

            // Check if the face ID exists
            var snapshot = faceIdDocRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("Face ID not found");
            }

            // Use batch to delete face ID and update isEnrolledFaceId status
            WriteBatch batch = firestore.batch();
            batch.delete(faceIdDocRef);
            batch.update(userDocRef, "isEnrolledFaceId", false);
            batch.commit().get();

            return new SingleResponse("Face ID deleted successfully");
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        }
    }

    /**
     * Updates the user information in Firestore based on the provided userId and UpdateUserRequest.
     *
     * @param user The UpdateUserRequest object containing the new user information.
     * @param userId The ID of the user to be updated.
     * @return UserResponse object containing the updated user information.
     * @throws NotFoundException if the user with the given userId does not exist.
     * @throws ConflictException if the new email or phone number is already taken by another user.
     * @throws InternalServerErrorException if there is an internal server error during the update process.
     */
    public UserResponse updateUser(UpdateUserRequest user, String userId) {
        try {
            // Get reference to the user document in Firestore
            CollectionReference usersCollection = firestore.collection("users");
            DocumentReference docRef = usersCollection.document(userId);

            // Check if the user exists
            var snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Initialize UserResponse from existing Firestore data
            UserResponse userResponse = new UserResponse();
            userResponse.setFirstName(snapshot.getString("firstName"));
            userResponse.setLastName(snapshot.getString("lastName"));
            userResponse.setEmail(snapshot.getString("email"));
            userResponse.setPhone(snapshot.getString("phone"));
            userResponse.setAvatar(snapshot.getString("avatar"));
            userResponse.setSensorId(snapshot.getString("sensorId"));

            // Create a batch write
            WriteBatch batch = firestore.batch();
            boolean hasUpdates = false;

            // Update firstName if a new value is provided
            if (user.getFirstName() != null) {
                batch.update(docRef, "firstName", user.getFirstName());
                userResponse.setFirstName(user.getFirstName());
                hasUpdates = true;
            }

            // Update lastName if a new value is provided
            if (user.getLastName() != null) {
                batch.update(docRef, "lastName", user.getLastName());
                userResponse.setLastName(user.getLastName());
                hasUpdates = true;
            }

            // Check if email is unique before updating
            if (user.getEmail() != null && !user.getEmail().equals(userResponse.getEmail())) {
                if (isEmailTaken(usersCollection, user.getEmail(), userId)) {
                    throw new ConflictException("Email is already taken");
                }
                batch.update(docRef, "email", user.getEmail());
                userResponse.setEmail(user.getEmail());
                hasUpdates = true;
            }

            // Check if phone is unique before updating
            if (user.getPhone() != null && !user.getPhone().equals(userResponse.getPhone())) {
                if (isPhoneTaken(usersCollection, user.getPhone(), userId)) {
                    throw new ConflictException("Phone is already taken");
                }
                batch.update(docRef, "phone", user.getPhone());
                userResponse.setPhone(user.getPhone());
                hasUpdates = true;
            }

            // Upload new avatar if provided
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                String avatarUrl = cloudinaryUtil.uploadImage(user.getAvatar(), userId);
                batch.update(docRef, "avatar", avatarUrl);
                userResponse.setAvatar(avatarUrl);
                hasUpdates = true;
            }

            // Commit batch updates if there are any changes
            if (hasUpdates) {
                batch.commit().get();
            }

            return userResponse;
        } catch (IOException | ExecutionException e) {
            throw new InternalServerErrorException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        }
    }

    /**
     * Retrieves user information based on the provided user ID.
     *
     * @param userId the ID of the user whose information is to be retrieved
     * @return a UserResponse object containing the user's details
     * @throws NotFoundException if the user is not found in the Firestore database
     * @throws InternalServerErrorException if an internal server error occurs during the process
     */
    public UserResponse getUserInfo(String userId) {
        try {
            // Get reference to the user document in Firestore
            DocumentReference docRef = firestore.collection("users").document(userId);

            // Check if the user exists
            var snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Extract user details
            String firstName = snapshot.getString("firstName");
            String lastName = snapshot.getString("lastName");
            String email = snapshot.getString("email");
            String phone = snapshot.getString("phone");
            String avatar = snapshot.getString("avatar");
            String sensorId = snapshot.getString("sensorId");
            Boolean isEnrolledFaceIdObj = snapshot.getBoolean("isEnrolledFaceId");
            boolean isEnrolledFaceId = isEnrolledFaceIdObj != null && isEnrolledFaceIdObj;

            return new UserResponse(docRef.getId(), firstName, lastName, email, phone, avatar, sensorId, isEnrolledFaceId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Changes the password for a user.
     *
     * @param request the request containing the current and new passwords
     * @param userId the ID of the user whose password is to be changed
     * @return a SingleResponse indicating the result of the password change operation
     * @throws NotFoundException if the user is not found
     * @throws UnauthorizedException if the current password is invalid
     * @throws InternalServerErrorException if an internal server error occurs
     */
    public SingleResponse changePassword(ChangePasswordRequest request, String userId) {
        try {
            // Get reference to the user document in Firestore
            DocumentReference docRef = firestore.collection("users").document(userId);

            // Check if the user exists
            var snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Extract the stored password
            String storedPassword = snapshot.getString("password");

            // Validate the old password
            if (!Argon.compare(request.getCurrPassword(), storedPassword)) {
                throw new UnauthorizedException("Current password is incorrect");
            }

            // Hash the new password
            String hashedPassword = Argon.hashPassword(request.getNewPassword());

            // Update the password in Firestore
            docRef.update("password", hashedPassword).get();

            return new SingleResponse("Password changed successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Retrieves the list of notifications for a specific user based on their subscribed sensor.
     *
     * @param userId The ID of the user whose notifications are to be retrieved.
     * @return A list of {@link NotificationResponse} objects containing the user's notifications.
     * @throws NotFoundException If the user or the associated sensor is not found.
     * @throws BadRequestException If the user has not subscribed to any sensor.
     * @throws InternalServerErrorException If an error occurs while fetching data from Firestore.
     */
    public PaginationResponse<NotificationResponse> getUserNotifications(String userId, Integer page, Integer limit) {
        try {
            // Get reference to the user document in Firestore
            DocumentReference docRef = firestore.collection("users").document(userId);

            // Check if the user exists
            var snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Extract the sensor ID
            String sensorId = snapshot.getString("sensorId");
            if (sensorId == null) {
                throw new BadRequestException("User has not subscribed to any sensor.");
            }

            // Check if the sensor ID is valid
            DocumentReference sensorDoc = firestore.collection("sensors").document(sensorId);
            DocumentSnapshot sensorSnapshot = sensorDoc.get().get();
            if (!sensorSnapshot.exists()) {
                throw new NotFoundException("Sensor not found");
            }

            int _page = page != null ? page : 1;
            int _limit = limit != null ? limit : 10;
            return notificationService.getNotifications(sensorId, _page, _limit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Creates a new mode configuration for a user.
     *
     * @param userId The ID of the user for whom the mode configuration is being created.
     * @param request The request object containing the details of the mode configuration.
     * @return A SingleResponse object indicating the success of the operation.
     * @throws NotFoundException If the user with the given ID does not exist.
     * @throws ConflictException If a mode configuration with the same name already exists for the user.
     * @throws InternalServerErrorException If an error occurs during the operation.
     */
    public SingleResponse createModeConfig(String userId, CreateModeConfigRequest request) {
        try {
            // Get reference to the user document in Firestore
            DocumentReference docRef = firestore.collection("users").document(userId);

            // Check if the user exists
            var snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Check if a mode configuration with the same name already exists for the user
            CollectionReference modeConfigsCollection = firestore.collection("mode_configs");
            Query nameQuery = modeConfigsCollection.whereEqualTo("userId", userId)
                                                   .whereEqualTo("name", request.getName());
            List<QueryDocumentSnapshot> existingConfigs = nameQuery.get().get().getDocuments();

            if (!existingConfigs.isEmpty()) {
                throw new ConflictException("Mode configuration with the same name already exists");
            }

            // Create a new mode configuration document
            DocumentReference modeConfigDocRef = modeConfigsCollection.document();
            ModeConfigResponse modeConfig = new ModeConfigResponse(
                    modeConfigDocRef.getId(),
                    userId,
                    request.getName(),
                    request.getLedMode(),
                    request.getBrightness(),
                    request.getFanMode()
            );
            modeConfigDocRef.set(modeConfig).get();

            return new SingleResponse("Mode configuration created successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Retrieves the mode configurations associated with a specific user.
     *
     * @param userId The ID of the user whose mode configurations are to be retrieved.
     * @return A list of {@link ModeConfigResponse} objects representing the user's mode configurations.
     * @throws NotFoundException If the user with the specified ID does not exist.
     * @throws InternalServerErrorException If an error occurs while retrieving data from Firestore.
     */
    public List<ModeConfigResponse> getUserModeConfigs(String userId) {
        try {
            // Get reference to the user document in Firestore
            DocumentReference docRef = firestore.collection("users").document(userId);

            // Check if the user exists
            var snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Retrieve the mode configurations for the user
            CollectionReference modeConfigsCollection = firestore.collection("mode_configs");
            Query userModeConfigsQuery = modeConfigsCollection.whereEqualTo("userId", userId);
            List<QueryDocumentSnapshot> modeConfigDocs = userModeConfigsQuery.get().get().getDocuments();

            return modeConfigDocs.stream().map(doc -> {
                String id = doc.getId();
                String uid = doc.getString("userId");
                String name = doc.getString("name");
                long ledMode = safeGetLong(doc, "ledMode", 0);
                long brightness = safeGetLong(doc, "brightness", 0);
                long fanMode = safeGetLong(doc, "fanMode", 0);
                return new ModeConfigResponse(id, uid, name, ledMode, brightness, fanMode);
            }).toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Retrieves the mode configuration for a specific user and mode.
     *
     * @param userId The ID of the user whose mode configuration is being retrieved.
     * @param modeId The ID of the mode configuration to retrieve.
     * @return A {@link ModeConfigResponse} object containing the mode configuration details.
     * @throws NotFoundException If the user or mode configuration is not found.
     * @throws ForbiddenException If the mode configuration does not belong to the user.
     * @throws InternalServerErrorException If an error occurs during the retrieval process.
     */
    public ModeConfigResponse getModeConfig(String userId, String modeId) {
        try {
            // Get reference to the user document in Firestore
            DocumentReference docRef = firestore.collection("users").document(userId);

            // Check if the user exists
            var snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Retrieve the mode configuration for the user
            DocumentReference modeConfigDocRef = firestore.collection("mode_configs").document(modeId);
            DocumentSnapshot modeConfigDoc = modeConfigDocRef.get().get();

            if (!modeConfigDoc.exists()) {
                throw new NotFoundException("Mode configuration not found");
            }

            // Check if the mode configuration belongs to the user
            String modeUserId = modeConfigDoc.getString("userId");
            if (!userId.equals(modeUserId)) {
                throw new ForbiddenException("You do not have permission to access this mode configuration");
            }

            String uid = modeConfigDoc.getString("userId");
            String name = modeConfigDoc.getString("name");
            long ledMode = safeGetLong(modeConfigDoc, "ledMode", 0);
            long brightness = safeGetLong(modeConfigDoc, "brightness", 0);
            long fanMode = safeGetLong(modeConfigDoc, "fanMode", 0);

            return new ModeConfigResponse(modeId, uid, name, ledMode, brightness, fanMode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Updates the mode configuration for a specific user.
     *
     * @param userId  The ID of the user whose mode configuration is being updated.
     * @param modeId  The ID of the mode configuration to update.
     * @param request The request object containing the updated mode configuration details.
     * @return A SingleResponse indicating the success of the update operation.
     * @throws NotFoundException         If the user or mode configuration is not found.
     * @throws ForbiddenException        If the mode configuration does not belong to the user.
     * @throws InternalServerErrorException If an error occurs during the update process.
     */
    public SingleResponse updateModeConfig(String userId, String modeId, UpdateModeConfigRequest request) {
        try {
            // Get reference to the user document in Firestore
            DocumentReference docRef = firestore.collection("users").document(userId);

            // Check if the user exists
            var snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Retrieve the mode configuration for the user
            DocumentReference modeConfigDocRef = firestore.collection("mode_configs").document(modeId);
            DocumentSnapshot modeConfigDoc = modeConfigDocRef.get().get();

            if (!modeConfigDoc.exists()) {
                throw new NotFoundException("Mode configuration not found");
            }

            // Check if the mode configuration belongs to the user
            String modeUserId = modeConfigDoc.getString("userId");
            if (!userId.equals(modeUserId)) {
                throw new ForbiddenException("You do not have permission to access this mode configuration");
            }

            // Create a batch write
            WriteBatch batch = firestore.batch();
            boolean hasUpdates = false;

            // Update fields if new values are provided
            if (request.getName() != null) {
                batch.update(modeConfigDocRef, "name", request.getName());
                hasUpdates = true;
            }

            if (request.getLedMode() != null) {
                batch.update(modeConfigDocRef, "ledMode", request.getLedMode());
                hasUpdates = true;
            }

            if (request.getBrightness() != null) {
                batch.update(modeConfigDocRef, "brightness", request.getBrightness());
                hasUpdates = true;
            }

            if (request.getFanMode() != null) {
                batch.update(modeConfigDocRef, "fanMode", request.getFanMode());
                hasUpdates = true;
            }

            // Commit batch updates if there are any changes
            if (hasUpdates) {
                batch.commit().get();
            }

            return new SingleResponse("Mode configuration updated successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Deletes a mode configuration for a specific user.
     *
     * @param userId The ID of the user who owns the mode configuration.
     * @param modeId The ID of the mode configuration to be deleted.
     * @return A SingleResponse indicating the success of the operation.
     * @throws NotFoundException If the user or the mode configuration does not exist.
     * @throws ForbiddenException If the mode configuration does not belong to the specified user.
     * @throws InternalServerErrorException If an error occurs during the deletion process.
     */
    public SingleResponse deleteModeConfig(String userId, String modeId) {
        try {
            // Get reference to the user document in Firestore
            DocumentReference docRef = firestore.collection("users").document(userId);

            // Check if the user exists
            var snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Retrieve the mode configuration for the user
            DocumentReference modeConfigDocRef = firestore.collection("mode_configs").document(modeId);
            DocumentSnapshot modeConfigDoc = modeConfigDocRef.get().get();

            if (!modeConfigDoc.exists()) {
                throw new NotFoundException("Mode configuration not found");
            }

            // Check if the mode configuration belongs to the user
            String modeUserId = modeConfigDoc.getString("userId");
            if (!userId.equals(modeUserId)) {
                throw new ForbiddenException("You do not have permission to access this mode configuration");
            }

            // Delete the mode configuration document
            modeConfigDocRef.delete().get();

            return new SingleResponse("Mode configuration deleted successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Activates a mode configuration for a specific user by sending the configuration
     * details to the associated sensor.
     *
     * @param userId The ID of the user for whom the mode configuration is to be activated.
     * @param modeId The ID of the mode configuration to be activated.
     * @return A {@link SingleResponse} indicating the success of the operation.
     * @throws NotFoundException If the user, mode configuration, or sensor is not found.
     * @throws ForbiddenException If the mode configuration does not belong to the specified user.
     * @throws InternalServerErrorException If an error occurs during the execution of the operation.
     */
    public SingleResponse activateModeConfig(String userId, String modeId) {
        try {
            // Get reference to the user document in Firestore
            DocumentReference docRef = firestore.collection("users").document(userId);

            // Check if the user exists
            var snapshot = docRef.get().get();
            if (!snapshot.exists()) {
                throw new NotFoundException("User not found");
            }

            // Retrieve the mode configuration for the user
            DocumentReference modeConfigDocRef = firestore.collection("mode_configs").document(modeId);
            DocumentSnapshot modeConfigDoc = modeConfigDocRef.get().get();

            if (!modeConfigDoc.exists()) {
                throw new NotFoundException("Mode configuration not found");
            }

            // Check if the mode configuration belongs to the user
            String modeUserId = modeConfigDoc.getString("userId");
            if (!userId.equals(modeUserId)) {
                throw new ForbiddenException("You do not have permission to access this mode configuration");
            }

            // Retrieve the mode configuration details
            long ledMode = safeGetLong(modeConfigDoc, "ledMode", 0);
            long brightness = safeGetLong(modeConfigDoc, "brightness", 0);
            long fanMode = safeGetLong(modeConfigDoc, "fanMode", 0);

            String sensorId = snapshot.getString("sensorId");
            if (sensorId == null) {
                throw new NotFoundException("Sensor not found");
            }

            // Send the mode configuration to the sensor
            sensorDataService.forceControl(sensorId, ledMode, fanMode, brightness);

            return new SingleResponse("Mode configuration activated successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Checks if the given email is already taken by another user in the Firestore collection.
     *
     * @param collection the Firestore collection reference to query
     * @param email the email to check for existence
     * @param userId the user ID to exclude from the check (can be null)
     * @return true if the email is taken by another user, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting for the query to complete
     * @throws ExecutionException if an error occurs while executing the query
     */
    private boolean isEmailTaken(CollectionReference collection, String email, String userId) throws InterruptedException, ExecutionException {
        // Query Firestore for user with the given email
        Query emailQuery = collection.whereEqualTo("email", email);
        ApiFuture<QuerySnapshot> emailQuerySnapshot = emailQuery.get();
        List<QueryDocumentSnapshot> documents = emailQuerySnapshot.get().getDocuments();

        // Validate user existence
        if (documents.isEmpty()) {
            return false;
        }

        // Retrieve the user document
        DocumentSnapshot userDoc = documents.get(0);
        String storedUserId = userDoc.getId();

        return userId == null || !storedUserId.equals(userId);
    }

    /**
     * Checks if a phone number is already taken by another user in the Firestore collection.
     *
     * @param collection The Firestore collection reference to query.
     * @param phone The phone number to check for existence.
     * @param userId The user ID to exclude from the check (can be null).
     * @return true if the phone number is taken by another user, false otherwise.
     * @throws InterruptedException If the Firestore query is interrupted.
     * @throws ExecutionException If the Firestore query fails.
     */
    private boolean isPhoneTaken(CollectionReference collection, String phone, String userId) throws InterruptedException, ExecutionException {
        // Query Firestore for user with the given phone
        Query phoneQuery = collection.whereEqualTo("phone", phone);
        ApiFuture<QuerySnapshot> phoneQuerySnapshot = phoneQuery.get();
        List<QueryDocumentSnapshot> documents = phoneQuerySnapshot.get().getDocuments();

        // Validate user existence
        if (documents.isEmpty()) {
            return false;
        }

        // Retrieve the user document
        DocumentSnapshot userDoc = documents.get(0);
        String storedUserId = userDoc.getId();

        return userId == null || !storedUserId.equals(userId);
    }

    /**
     * Safely retrieves a long value from a DocumentSnapshot for the specified field.
     * If the field is null or does not exist, the provided default value is returned.
     *
     * @param doc          The DocumentSnapshot object to retrieve the value from.
     * @param field        The name of the field to retrieve.
     * @param defaultValue The default value to return if the field is null or does not exist.
     * @return The long value of the specified field, or the default value if the field is null or does not exist.
     */
    private long safeGetLong(DocumentSnapshot doc, String field, long defaultValue) {
        Long value = doc.getLong(field);
        return value != null ? value : defaultValue;
    }

}
