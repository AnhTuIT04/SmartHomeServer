package hcmut.smart_home.dto.FaceEmbedding;

import java.util.List;

public class FaceEmbedding {
    private List<Double> embedding;
    private int dimensions;
    
    // Default constructor for JSON deserialization
    public FaceEmbedding() {
    }
    
    public FaceEmbedding(List<Double> embedding, int dimensions) {
        this.embedding = embedding;
        this.dimensions = dimensions;
    }
    
    public List<Double> getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }
    
    public int getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }
    
    @Override
    public String toString() {
        return "FaceEmbedding{" +
                "embedding=" + (embedding != null ? embedding.subList(0, Math.min(5, embedding.size())).toString() + "... (truncated)" : "null") +
                ", dimensions=" + dimensions +
                '}';
    }
}
