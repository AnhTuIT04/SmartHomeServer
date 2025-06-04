package hcmut.smart_home.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;

import hcmut.smart_home.dto.PaginationResponse;
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
    public PaginationResponse<NotificationResponse> getNotifications(String sensorId, int page, int limit) {
        try {
            List<NotificationResponse> notifications = new ArrayList<>();
            // Fetch all notifications for the sensor, sorted ascending by timestamp (or createdAt)
            var querySnapshot = firestore.collection("notifications")
                    .whereEqualTo("sensorId", sensorId)
                    .orderBy("timestamp", com.google.cloud.firestore.Query.Direction.DESCENDING)
                    .get()
                    .get();

            int total = querySnapshot.size();
            int fromIndex = Math.max(0, (page - 1) * limit);
            int toIndex = Math.min(fromIndex + limit, total);

            querySnapshot.getDocuments().subList(fromIndex, toIndex)
                    .forEach(doc -> notifications.add(new NotificationResponse(doc.getData())));

            boolean hasNextPage = toIndex < total;
            boolean hasPrevPage = fromIndex > 0;

            NotificationResponse[] dataArr = notifications.toArray(NotificationResponse[]::new);
            return new PaginationResponse<>(dataArr, page, limit, total, hasNextPage, hasPrevPage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException();
        } catch (ExecutionException e) {
            throw new InternalServerErrorException();
        }
    }
}
