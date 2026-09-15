package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.AuthDtos.AuthRequest;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.entity.UserType;
import br.com.sprint1.challenge.exception.InvalidCredentialsException;
import br.com.sprint1.challenge.exception.UserLockedException;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.repository.UserTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AuthServiceLockoutIntegrationTest {

    private static final String EMAIL = "lockout@example.com";
    private static final String PASSWORD = "Password1!";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTypeRepository userTypeRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        UserType userType = userTypeRepository.findByType("USER")
                .orElseThrow();
        User user = new User();
        user.setUsername("lockout-user");
        user.setEmail(EMAIL);
        user.setCpf("98765432100");
        user.setHashedPassword(BCrypt.hashpw(PASSWORD, BCrypt.gensalt(10)));
        user.setUserType(userType);
        userRepository.save(user);
    }

    @Test
    void cincoFalhasPersistemBloqueioMesmoComExcecao() {
        AuthRequest wrongPassword = new AuthRequest(EMAIL, "WrongPassword1!");

        IntStream.range(0, 5).forEach(ignored ->
                assertThrows(InvalidCredentialsException.class,
                        () -> authService.authenticate(wrongPassword)));

        assertBlocked();
    }

    @Test
    void cincoFalhasConcorrentesPersistemBloqueio() throws Exception {
        AuthRequest wrongPassword = new AuthRequest(EMAIL, "WrongPassword1!");
        CountDownLatch ready = new CountDownLatch(5);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(5)) {
            List<Future<Class<? extends Throwable>>> results = new ArrayList<>();
            IntStream.range(0, 5).forEach(ignored -> results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    authService.authenticate(wrongPassword);
                    return null;
                } catch (Throwable throwable) {
                    return throwable.getClass();
                }
            })));

            ready.await();
            start.countDown();
            for (Future<Class<? extends Throwable>> result : results) {
                assertEquals(InvalidCredentialsException.class, result.get());
            }
        }

        assertBlocked();
    }

    private void assertBlocked() {
        User persisted = userRepository.findByEmail(EMAIL).orElseThrow();
        assertEquals(5, persisted.getFailedLoginAttempts());
        assertNotNull(persisted.getLockedUntil());
        assertThrows(UserLockedException.class,
                () -> authService.authenticate(new AuthRequest(EMAIL, PASSWORD)));
    }
}
