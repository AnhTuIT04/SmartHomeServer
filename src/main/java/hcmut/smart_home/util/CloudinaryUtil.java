package hcmut.smart_home.util;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

@Component
public class CloudinaryUtil {
    
    private final Cloudinary cloudinary;

    public CloudinaryUtil(@Value("${cloudinary.url}") String cloudinaryUrl) {
        this.cloudinary = new Cloudinary(cloudinaryUrl);
    }

    public String uploadImage(MultipartFile file, String userId) throws IOException {
        var uploadResult = cloudinary.uploader().upload(file.getBytes(),
            ObjectUtils.asMap(
                "folder", "smart-home",
                "public_id", userId,
                "transformation", new Transformation<>()
                    .width(500)
                    .height(500)
                    .crop("fill")
                    .gravity("auto")
                    .fetchFormat("auto")
                    .quality("auto")
                )
        );

        return uploadResult.get("secure_url").toString();
    }

    public boolean deleteImage(String publicId) throws IOException {
        var result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        return "ok".equals(result.get("result"));
    }
}
