package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.AuthDtos.AuthRequest;
import br.com.sprint1.challenge.dto.AuthDtos.AuthResponse;
import br.com.sprint1.challenge.dto.AuthDtos.RefreshTokenRequest;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.entity.UserType;
import br.com.sprint1.challenge.exception.InvalidTokenException;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.repository.UserTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthServiceRefreshIntegrationTest {

    private static final String EMAIL = "refresh@example.com";
    private static final String PASSWORD = "Password1!";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTypeRepository userTypeRepository;

    private String refreshToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        UserType userType = userTypeRepository.findByType("USER").orElseThrow();
        User user = new User();
        user.setUsername("refresh-user");
        user.setEmail(EMAIL);
        user.setCpf("87654321099");
        user.setHashedPassword(BCrypt.hashpw(PASSWORD, BCrypt.gensalt(10)));
        user.setUserType(userType);
        userRepository.save(user);

        AuthResponse login = authService.authenticate(new AuthRequest(EMAIL, PASSWORD));
        refreshToken = login.refreshToken();
    }

    @Test
    void refreshRotacionaTokenEInvalidaAnterior() {
        LocalDateTime before = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusDays(30);
        var response = authService.refreshToken(new RefreshTokenRequest(refreshToken));
        LocalDateTime after = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusDays(30);

        assertNotEquals(refreshToken, response.refreshToken());
        assertThrows(InvalidTokenException.class,
                () -> authService.refreshToken(new RefreshTokenRequest(refreshToken)));

        User persisted = userRepository.findByEmail(EMAIL).orElseThrow();
        assertTrue(!persisted.getRefreshTokenExpiresAt().isBefore(before));
        assertTrue(!persisted.getRefreshTokenExpiresAt().isAfter(after));
    }

    @Test
    void refreshConcorrentePermiteSomenteUmaRotacao() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Class<? extends Throwable>>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        authService.refreshToken(new RefreshTokenRequest(refreshToken));
                        return null;
                    } catch (Throwable throwable) {
                        return throwable.getClass();
                    }
                }));
            }

            ready.await();
            start.countDown();
            long successes = 0;
            long failures = 0;
            for (Future<Class<? extends Throwable>> result : results) {
                if (result.get() == null) {
                    successes++;
                } else if (result.get() == InvalidTokenException.class) {
                    failures++;
                }
            }
            assertEquals(1, successes);
            assertEquals(1, failures);
        }
    }
}
