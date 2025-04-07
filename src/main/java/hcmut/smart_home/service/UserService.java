package hcmut.smart_home.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;

import hcmut.smart_home.dto.SingleResponse;
import hcmut.smart_home.dto.notification.NotificationResponse;
import hcmut.smart_home.dto.user.AuthResponse;
import hcmut.smart_home.dto.user.ChangePasswordRequest;
import hcmut.smart_home.dto.user.CreateUserRequest;
import hcmut.smart_home.dto.user.LoginUserRequest;
import hcmut.smart_home.dto.user.TokenRequest;
import hcmut.smart_home.dto.user.TokenResponse;
import hcmut.smart_home.dto.user.UpdateUserRequest;
import hcmut.smart_home.dto.user.UserResponse;
import hcmut.smart_home.exception.BadRequestException;
import hcmut.smart_home.exception.ConflictException;
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

    public UserService(Firestore firestore, Jwt jwt, CloudinaryUtil cloudinaryUtil, NotificationService notificationService) {
        this.firestore = firestore;
        this.jwt = jwt;
        this.cloudinaryUtil = cloudinaryUtil;
        this.notificationService = notificationService;
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

            // Generate authentication tokens
            String accessToken = jwt.generateAccessToken(userId);
            String refreshToken = jwt.generateRefreshToken(userId);

            // Return the user response
            return new AuthResponse(user, userId, firstName, lastName, phone, avatar, sensorId, accessToken, refreshToken);

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

            return new UserResponse(docRef.getId(), firstName, lastName, email, phone, avatar, sensorId);
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

    public List<NotificationResponse> getUserNotifications(String userId) {
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

            return notificationService.getNotifications(sensorId);

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

}
