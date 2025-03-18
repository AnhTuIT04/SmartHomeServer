package hcmut.smart_home.util;

import java.security.Key;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class Jwt {

    @Value("${jwt.secret}")
    private String SECRET_KEY;
    @Value("${jwt.access-token-expiration}")
    private long ACCESS_TOKEN_EXPIRATION;
    @Value("${jwt.refresh-token-expiration}")
    private long REFRESH_TOKEN_EXPIRATION;

    private Key getSigningKey() {
        if (SECRET_KEY == null || SECRET_KEY.isEmpty()) {
            throw new IllegalStateException("SECRET_KEY is not set");
        }
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));
    }

    public String generateAccessToken(String id) {
        return Jwts.builder()
                .subject(id)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String id) {
        return Jwts.builder()
                .subject(id)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            String type = Jwts.parser()
                                .verifyWith((SecretKey) getSigningKey())
                                .build()
                                .parseSignedClaims(token)
                                .getPayload()
                                .get("type", String.class);

            return "access".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            String type = Jwts.parser()
                                .verifyWith((SecretKey) getSigningKey())
                                .build()
                                .parseSignedClaims(token)
                                .getPayload()
                                .get("type", String.class);

            return "refresh".equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractId(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
