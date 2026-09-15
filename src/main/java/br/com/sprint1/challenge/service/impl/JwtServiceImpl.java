package br.com.sprint1.challenge.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import br.com.sprint1.challenge.config.JwtProperties;
import br.com.sprint1.challenge.service.JwtService;
import io.jsonwebtoken.Claims;
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
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date exp = new Date(now.getTime() + (jwtProperties.getExpirationMinutes() * 60 * 1000));

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .claim("type", "ACCESS")
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String generateRefreshToken(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date exp = new Date(now.getTime() + (jwtProperties.getRefreshTokenExpiryDays() * 24L * 60 * 60 * 1000));

        return Jwts.builder()
                .subject(userId)
                .claim("type", "REFRESH")
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(exp)
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String generatePasswordResetToken(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date exp = new Date(now.getTime() + (15 * 60 * 1000)); // 15 minutes

        return Jwts.builder()
                .subject(userId)
                .claim("type", "PASSWORD_RESET")
                .claim("purpose", "PASSWORD_RESET")
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(exp)
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Claims parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(jwtProperties.getIssuer())
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