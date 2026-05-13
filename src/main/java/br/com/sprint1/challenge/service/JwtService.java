package br.com.sprint1.challenge.service;

import io.jsonwebtoken.Claims;

public interface JwtService {
    String generatePreAuthToken(String userId, String email);
    Claims parse(String token);
}