package hcmut.smart_home.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import hcmut.smart_home.dto.FaceEmbedding.FaceEmbedding;

@Service
public class FaceEmbeddingService {

    @Value("${face.embedding.threshold}")
    private double threshold;

    @Value("${face.embedding.server.url}")
    private String faceEmbeddingServerUrl;

    private final RestTemplate restTemplate;

    public FaceEmbeddingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public double getThreshold() {
        return threshold;
    }

    /**
     * Calculates the cosine similarity between two face embeddings.
     *
     * <p>
     * The similarity is computed as the cosine of the angle between the two embedding vectors.
     * The result ranges from -1 (completely dissimilar) to 1 (identical).
     * </p>
     *
     * @param embedding1 the first embedding vector (must be non-null and have the same length as embedding2)
     * @param embedding2 the second embedding vector (must be non-null and have the same length as embedding1)
     * @return the cosine similarity between the two embeddings
     * @throws IllegalArgumentException if either embedding is null or their lengths do not match
     */
    public double calculateSimilarity(List<Double> embedding1, List<Double> embedding2) {
        if (embedding1 == null || embedding2 == null || embedding1.size() != embedding2.size()) {
            throw new IllegalArgumentException("Embeddings must be non-null and have the same dimensions");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.size(); i++) {
            dotProduct += embedding1.get(i) * embedding2.get(i);
            norm1 += embedding1.get(i)  * embedding1.get(i) ;
            norm2 += embedding2.get(i)  * embedding2.get(i) ;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Sends an image file to the Face Embedding server to extract face embeddings.
     *
     * <p>This method prepares a multipart/form-data HTTP request containing the provided image file,
     * sends it to the configured Face Embedding server endpoint, and returns the extracted embedding
     * information encapsulated in a {@link FaceEmbedding} object.</p>
     *
     * @param imageFile the image file containing a face, provided as a {@link MultipartFile}
     * @return a {@link FaceEmbedding} containing the extracted face embedding data
     * @throws RuntimeException if there is an error reading the image file or communicating with the Face Embedding server
     */
    public FaceEmbedding getEmbedding(MultipartFile imageFile) {
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Prepare the request body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource;
            try {
                resource = new ByteArrayResource(imageFile.getBytes()) {
                    @Override
                    public String getFilename() {
                        return imageFile.getOriginalFilename();
                    }
                };
            } catch (IOException e) {
                throw new RuntimeException("Error occurred while reading image file bytes", e);
            }
            body.add("image", resource);
            
            // Create the request entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Send the request to the Face Embedding server
            String url = faceEmbeddingServerUrl + "/api/extract-embedding";
            
            ResponseEntity<FaceEmbedding> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    FaceEmbedding.class
            );
            
            return response.getBody();
        } catch (RestClientException e) {
            throw new RuntimeException("Error occurred while communicating with the Face Embedding server", e);
        }
    }
}
