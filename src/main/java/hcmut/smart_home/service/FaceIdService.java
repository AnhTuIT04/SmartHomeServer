package hcmut.smart_home.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import hcmut.smart_home.dto.user.FaceIDRequest;

@Service
public class FaceIdService {
    @Value("${face-id.threshold}")
    private double threshold;

    public double getThreshold() {
        return threshold;
    }

    /**
     * Computes the Euclidean distance between the embeddings of two FaceIDRequest objects.
     *
     * @param faceIDRequest1 the first FaceIDRequest containing an embedding vector
     * @param faceIDRequest2 the second FaceIDRequest containing an embedding vector
     * @return the Euclidean distance between the two embedding vectors
     */
    public double computeEuclideanDistance(FaceIDRequest faceIDRequest1, FaceIDRequest faceIDRequest2) {
        List<Double> embedding1 = faceIDRequest1.getEmbedding();
        List<Double> embedding2 = faceIDRequest2.getEmbedding();
        double sum = 0.0;
        for (int i = 0; i < embedding1.size(); i++) {
            double diff = embedding1.get(i) - embedding2.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
