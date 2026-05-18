package br.com.sprint1.challenge.service;

import io.jsonwebtoken.Claims;

public interface JwtService {
    default String generateToken(String userId, String email) {
        return generateToken(userId, email, "USER");
    }

    String generateToken(String userId, String email, String role);
    Claims parse(String token);
}