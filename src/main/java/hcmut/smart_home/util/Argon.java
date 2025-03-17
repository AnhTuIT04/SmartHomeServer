package hcmut.smart_home.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

public class Argon {
    private static final int SALT_LENGTH = 16; 
    private static final int HASH_LENGTH = 32; 
    private static final int ITERATIONS  = 3;   
    private static final int MEMORY_KB   = 65536;
    private static final int PARALLELISM = 2;  

    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        byte[] hash = generateArgon2Hash(password, salt);

        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean compare(String password, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) return false;

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            byte[] actualHash = generateArgon2Hash(password, salt);

            return Arrays.equals(expectedHash, actualHash);
        } catch (Exception e) {
            return false;  
        }
    }

    private static byte[] generateArgon2Hash(String password, byte[] salt) {
        Argon2Parameters.Builder paramsBuilder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withIterations(ITERATIONS)
                .withMemoryAsKB(MEMORY_KB)
                .withParallelism(PARALLELISM);

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(paramsBuilder.build());

        byte[] hash = new byte[HASH_LENGTH];
        generator.generateBytes(password.getBytes(StandardCharsets.UTF_8), hash);
        return hash;
    }
}
