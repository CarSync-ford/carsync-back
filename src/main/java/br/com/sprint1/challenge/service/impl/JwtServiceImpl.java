package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.config.JwtProperties;
import br.com.sprint1.challenge.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;

    public JwtServiceImpl(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public String generatePreAuthToken(String userId, String email) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        
        Date now = new Date();
        Date exp = new Date(now.getTime() + (jwtProperties.getPreAuthExpirationMinutes() * 60 * 1000));
        
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("scope", "pre-auth")
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Claims parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}