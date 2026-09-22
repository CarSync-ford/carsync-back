package br.com.sprint1.challenge.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import br.com.sprint1.challenge.config.JwtProperties;
import br.com.sprint1.challenge.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;

    public JwtServiceImpl(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void validateSecret() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 256 bits (32 characters) for HS256 security"
            );
        }
    }

    @Override
    public String generateToken(String userId, String email, String role) {
        long expirationMillis = jwtProperties.getExpirationMinutes() * 60 * 1000L;
        return buildToken(userId, Map.of("email", email, "role", role), expirationMillis, null);
    }

    @Override
    public String generateRefreshToken(String userId) {
        long expirationMillis = jwtProperties.getRefreshTokenExpiryDays() * 24L * 60 * 60 * 1000;
        return buildToken(userId, Map.of("type", "REFRESH"), expirationMillis, UUID.randomUUID().toString());
    }

    @Override
    public String generatePasswordResetToken(String userId) {
        long expirationMillis = 15 * 60 * 1000L; // 15 minutes
        Map<String, Object> claims = Map.of("type", "PASSWORD_RESET", "purpose", "PASSWORD_RESET");
        return buildToken(userId, claims, expirationMillis, UUID.randomUUID().toString());
    }

    /**
     * Template Method: concentra a montagem/assinatura do JWT hoje repetida em
     * cada método de geração (chave, claims, expiração, issuer). Evita a
     * duplicação anterior do {@code Jwts.builder()...signWith(...)} em 3 lugares.
     */
    private String buildToken(String subject, Map<String, Object> claims, long expirationMillis, String jwtId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMillis);

        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(exp);

        claims.forEach(builder::claim);

        if (jwtId != null) {
            builder.id(jwtId);
        }

        return builder.signWith(signingKey(), Jwts.SIG.HS256).compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public Claims parsePasswordResetToken(String token) {
        Claims claims = parse(token);
        String type = claims.get("type", String.class);
        if (!"PASSWORD_RESET".equals(type)) {
            throw new IllegalArgumentException("Invalid token type for password reset");
        }
        return claims;
    }
}