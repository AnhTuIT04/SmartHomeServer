package hcmut.smart_home.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.FirebaseDatabase;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp initializeFirebase(@Value("${firebase.credentials}") String encodedCredentials, @Value("${firebase.url}") String databaseUrl) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.getInstance().delete(); 
        }

        byte[] decodedCredentials = Base64.getDecoder().decode(encodedCredentials);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(decodedCredentials)))
                .setDatabaseUrl(databaseUrl) 
                .build();

        FirebaseApp.initializeApp(options);
        
        return FirebaseApp.getInstance();
    }

    @Bean
	public Firestore firestore(final FirebaseApp firebaseApp) {
		return FirestoreClient.getFirestore(firebaseApp);
	}

    @Bean
    public FirebaseDatabase firebaseDatabase(FirebaseApp firebaseApp) {
        return FirebaseDatabase.getInstance(firebaseApp);
    }
    
}
