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

import hcmut.smart_home.dto.user.AuthResponse;
import hcmut.smart_home.dto.user.CreateUserRequest;
import hcmut.smart_home.dto.user.LoginUserRequest;
import hcmut.smart_home.dto.user.TokenRequest;
import hcmut.smart_home.dto.user.TokenResponse;
import hcmut.smart_home.dto.user.UpdateUserRequest;
import hcmut.smart_home.dto.user.UserResponse;
import hcmut.smart_home.exception.AccountAlreadyExistsException;
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

    public UserService(Firestore firestore, Jwt jwt, CloudinaryUtil cloudinaryUtil) {
        this.firestore = firestore;
        this.jwt = jwt;
        this.cloudinaryUtil = cloudinaryUtil;
    }

    /**
     * Creates a new user in the Firestore database.
     *
     * @param user The request object containing user details.
     * @return UserResponse containing user details and authentication tokens.
     * @throws AccountAlreadyExistsException if the email or phone number already exists.
     * @throws RuntimeException if there is an issue interacting with Firestore.
     */
    public AuthResponse createUser(final CreateUserRequest user) {
        try {
            CollectionReference usersCollection = firestore.collection("users");

            // Validate if email or phone already exists
            if (isEmailTaken(user.getEmail(), null)) {
                throw new AccountAlreadyExistsException("Email already exists");
            }

            if (isPhoneTaken(user.getPhone(), null)) {
                throw new AccountAlreadyExistsException("Phone number already exists");
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
            return new AuthResponse(user, accessToken, refreshToken);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Internal Server Error", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Internal Server Error", e);
        }
    }

    /**
     * Authenticates a user using email and password.
     *
     * @param user The login request containing email and password.
     * @return UserResponse containing user details and authentication tokens.
     * @throws UnauthorizedException if the email or password is incorrect.
     * @throws RuntimeException if there is an issue fetching user data from Firestore.
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

            // Generate authentication tokens
            String accessToken = jwt.generateAccessToken(userId);
            String refreshToken = jwt.generateRefreshToken(userId);

            // Return the user response
            return new AuthResponse(user, firstName, lastName, phone, avatar, accessToken, refreshToken);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Internal Server Error", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Internal Server Error", e);
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
        if (!jwt.validateToken(refreshToken)) {
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
     * @throws AccountAlreadyExistsException if the new email or phone number is already taken by another user.
     * @throws RuntimeException if there is an internal server error during the update process.
     */
    public UserResponse updateUser(UpdateUserRequest user, String userId) {
        try {
            // Get reference to the user document in Firestore
            DocumentReference docRef = firestore.collection("users").document(userId);

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
                if (isEmailTaken(user.getEmail(), userId)) {
                    throw new AccountAlreadyExistsException("Email is already taken");
                }
                batch.update(docRef, "email", user.getEmail());
                userResponse.setEmail(user.getEmail());
                hasUpdates = true;
            }

            // Check if phone is unique before updating
            if (user.getPhone() != null && !user.getPhone().equals(userResponse.getPhone())) {
                if (isPhoneTaken(user.getPhone(), userId)) {
                    throw new AccountAlreadyExistsException("Phone is already taken");
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
            throw new RuntimeException("Internal Server Error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Internal Server Error", e);
        }
    }

    /**
     * Checks if the given email is already taken by another user.
     *
     * @param email  the email to check for existence
     * @param userId the ID of the user to exclude from the check
     * @return true if the email is taken by another user, false otherwise
     * @throws RuntimeException if an error occurs while querying the Firestore database
     */
    private boolean isEmailTaken(String email, String userId) {
        try {
            CollectionReference usersCollection = firestore.collection("users");

            // Query Firestore for user with the given email
            Query emailQuery = usersCollection.whereEqualTo("email", email);
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Internal Server Error", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Internal Server Error", e);
        }
    }

    /**
     * Checks if a phone number is already taken by another user.
     *
     * @param phone  the phone number to check
     * @param userId the ID of the user to exclude from the check
     * @return true if the phone number is taken by another user, false otherwise
     * @throws RuntimeException if there is an interruption or execution error during the Firestore query
     */
    private boolean isPhoneTaken(String phone, String userId) {
        try {
            CollectionReference usersCollection = firestore.collection("users");

            // Query Firestore for user with the given phone
            Query phoneQuery = usersCollection.whereEqualTo("phone", phone);
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Internal Server Error", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Internal Server Error", e);
        }
    }

}
