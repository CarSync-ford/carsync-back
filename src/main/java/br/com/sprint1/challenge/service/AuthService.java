package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.AuthDtos.AuthRequest;
import br.com.sprint1.challenge.dto.AuthDtos.AuthResponse;

public interface AuthService {
    AuthResponse authenticate(AuthRequest request);
}