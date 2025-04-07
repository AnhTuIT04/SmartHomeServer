package hcmut.smart_home.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;

import hcmut.smart_home.dto.notification.NotificationResponse;
import hcmut.smart_home.exception.InternalServerErrorException;

@Service
public class NotificationService {
    private final Firestore firestore;

    public NotificationService(Firestore firestore) {
        this.firestore = firestore;
    }

    public NotificationResponse createNotification(String sensorId) {
        DocumentReference docRef = firestore.collection("notifications").document();
        return new NotificationResponse(docRef.getId(), sensorId);
    }

    public void saveNotification(NotificationResponse notification) {
        try {
            DocumentReference docRef = firestore.collection("notifications").document(notification.getId());
            docRef.set(notification).get();
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
