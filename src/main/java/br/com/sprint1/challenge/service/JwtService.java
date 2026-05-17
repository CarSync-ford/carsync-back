package br.com.sprint1.challenge.service;

import io.jsonwebtoken.Claims;

public interface JwtService {
    String generateToken(String userId, String email);
    Claims parse(String token);
}