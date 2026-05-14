package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.dto.AuthDtos.AuthRequest;
import br.com.sprint1.challenge.dto.AuthDtos.AuthResponse;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.exception.InvalidCredentialsException;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.service.AuthService;
import br.com.sprint1.challenge.service.JwtService;
import jakarta.annotation.PostConstruct;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final int bcryptRounds;
    private String dummyHash;

    public AuthServiceImpl(
            UserRepository userRepository,
            JwtService jwtService,
            @Value("${spring.bcrypt.salt:10}") int bcryptRounds) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.bcryptRounds = bcryptRounds;
    }

    @PostConstruct
    public void init() {
        this.dummyHash = BCrypt.hashpw("__dummy__", BCrypt.gensalt(bcryptRounds));
    }

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        var userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            // Anti-timing: perform dummy hash check
            BCrypt.checkpw(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }

        User user = userOpt.get();
        
        if (!BCrypt.checkpw(request.password(), user.getHashedPassword())) {
            throw new InvalidCredentialsException();
        }

        userRepository.updateLastLoginById(user.getId());
        String token = jwtService.generatePreAuthToken(user.getId(), user.getEmail());
        return new AuthResponse(token);
    }
}