package hcmut.smart_home.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import hcmut.smart_home.dto.notification.NotificationResponse;
import hcmut.smart_home.exception.InternalServerErrorException;
import hcmut.smart_home.exception.NotFoundException;

@Service
public class NotificationService {
    private final Firestore firestore;

    public NotificationService(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Creates a new notification for a specified sensor.
     *
     * @param sensorId The ID of the sensor for which the notification is created.
     * @param type The type of the notification, represented by {@link NotificationResponse.NotificationType}.
     * @return A {@link NotificationResponse} object containing the details of the created notification.
     * @throws NotFoundException If the sensor with the specified ID does not exist.
     * @throws InternalServerErrorException If an error occurs during the notification creation process.
     */
    public NotificationResponse createNotification(String sensorId, NotificationResponse.Type type, NotificationResponse.Mode mode) {
        try {
            DocumentReference sensorDoc = firestore.collection("sensors").document(sensorId);
            DocumentSnapshot sensorSnapshot = sensorDoc.get().get();
            if (!sensorSnapshot.exists()) {
                throw new NotFoundException("Sensor not found");
            }

            DocumentReference docRef = firestore.collection("notifications").document();
            String notificationId = docRef.getId();
            NotificationResponse notification = new NotificationResponse(notificationId, sensorId, type, mode);
            docRef.set(notification).get();

            return notification;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    public NotificationResponse createNotification(NotificationResponse notification) {
        try {
            DocumentReference docRef = firestore.collection("notifications").document(notification.getId());
            docRef.set(notification).get();
            return notification;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Retrieves a list of notifications associated with a specific sensor ID.
     *
     * @param sensorId the ID of the sensor for which notifications are to be retrieved
     * @return a list of {@link NotificationResponse} objects representing the notifications
     * @throws InternalServerErrorException if an error occurs while fetching the notifications
     */
    public List<NotificationResponse> getNotifications(String sensorId) {
        try {
            List<NotificationResponse> notifications = new ArrayList<>();
            firestore.collection("notifications")
                    .whereEqualTo("sensorId", sensorId)
                    .get()
                    .get()
                    .forEach(doc -> notifications.add(doc.toObject(NotificationResponse.class)));

            return notifications;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }
}
