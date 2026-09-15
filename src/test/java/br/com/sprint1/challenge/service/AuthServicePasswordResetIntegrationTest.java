package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.AuthDtos.AuthRequest;
import br.com.sprint1.challenge.dto.AuthDtos.AuthResponse;
import br.com.sprint1.challenge.dto.AuthDtos.ForgotPasswordRequest;
import br.com.sprint1.challenge.dto.AuthDtos.ResetPasswordRequest;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.entity.UserType;
import br.com.sprint1.challenge.exception.InvalidCredentialsException;
import br.com.sprint1.challenge.exception.InvalidTokenException;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.repository.UserTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
class AuthServicePasswordResetIntegrationTest {

    private static final String EMAIL = "reset@example.com";
    private static final String OLD_PASSWORD = "OldPassword1!";
    private static final String NEW_PASSWORD = "NewPassword1!";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTypeRepository userTypeRepository;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private PasswordResetEmailService passwordResetEmailService;

    @BeforeEach
    void setUp() {
        reset(passwordResetEmailService);
        userRepository.deleteAll();

        UserType userType = userTypeRepository.findByType("USER").orElseThrow();
        User user = new User();
        user.setUsername("reset-user");
        user.setEmail(EMAIL);
        user.setCpf("76543210988");
        user.setHashedPassword(BCrypt.hashpw(OLD_PASSWORD, BCrypt.gensalt(10)));
        user.setUserType(userType);
        userRepository.save(user);
    }

    @Test
    void resetConsomeTokenRevogaRefreshEPermiteSomenteSenhaNova() {
        AuthResponse login = authService.authenticate(new AuthRequest(EMAIL, OLD_PASSWORD));
        String token = requestResetToken();

        User pending = userRepository.findByEmail(EMAIL).orElseThrow();
        assertEquals(64, pending.getPasswordResetTokenHash().length());
        assertNotEquals(token, pending.getPasswordResetTokenHash());
        assertNotNull(pending.getPasswordResetTokenExpiresAt());

        authService.resetPassword(new ResetPasswordRequest(token, NEW_PASSWORD));

        User resetUser = userRepository.findByEmail(EMAIL).orElseThrow();
        assertNull(resetUser.getPasswordResetTokenHash());
        assertNull(resetUser.getPasswordResetTokenExpiresAt());
        assertNull(resetUser.getRefreshToken());
        assertNull(resetUser.getRefreshTokenExpiresAt());
        assertThrows(InvalidTokenException.class,
                () -> authService.resetPassword(new ResetPasswordRequest(token, "AnotherPassword1!")));
        assertThrows(InvalidCredentialsException.class,
                () -> authService.authenticate(new AuthRequest(EMAIL, OLD_PASSWORD)));
        assertNotNull(authService.authenticate(new AuthRequest(EMAIL, NEW_PASSWORD)));
        assertNotNull(login.refreshToken());
    }

    @Test
    void novaSolicitacaoSubstituiTokenAnterior() {
        String oldToken = requestResetToken();
        String newToken = requestResetToken();

        assertNotEquals(oldToken, newToken);
        assertThrows(InvalidTokenException.class,
                () -> authService.resetPassword(new ResetPasswordRequest(oldToken, NEW_PASSWORD)));
        authService.resetPassword(new ResetPasswordRequest(newToken, NEW_PASSWORD));
    }

    @Test
    void tokenJwtValidoSemSolicitacaoERecusado() {
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        String unsolicitedToken = jwtFor(user.getId());

        assertThrows(InvalidTokenException.class,
                () -> authService.resetPassword(new ResetPasswordRequest(unsolicitedToken, NEW_PASSWORD)));
    }

    @Test
    void tokenExpiradoNoBancoERecusado() {
        String token = requestResetToken();
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        user.setPasswordResetTokenExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        userRepository.saveAndFlush(user);

        assertThrows(InvalidTokenException.class,
                () -> authService.resetPassword(new ResetPasswordRequest(token, NEW_PASSWORD)));
    }

    @Test
    void tokenMalformadoEMapeadoParaInvalidToken() {
        assertThrows(InvalidTokenException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("not-a-jwt", NEW_PASSWORD)));
    }

    @Test
    void usuarioInexistenteNaoGeraNemEnviaToken() {
        authService.forgotPassword(new ForgotPasswordRequest("missing@example.com"));

        verify(passwordResetEmailService, never())
                .sendPasswordResetEmail(eq("missing@example.com"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void falhaSmtpInvalidaSolicitacaoSemLancar() {
        doThrow(new MailSendException("smtp unavailable"))
                .when(passwordResetEmailService)
                .sendPasswordResetEmail(eq(EMAIL), org.mockito.ArgumentMatchers.anyString());

        authService.forgotPassword(new ForgotPasswordRequest(EMAIL));

        User persisted = userRepository.findByEmail(EMAIL).orElseThrow();
        assertNull(persisted.getPasswordResetTokenHash());
        assertNull(persisted.getPasswordResetTokenExpiresAt());
    }

    @Test
    void consumoConcorrentePermiteSomenteUmReset() throws Exception {
        String token = requestResetToken();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Class<? extends Throwable>>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        authService.resetPassword(new ResetPasswordRequest(token, NEW_PASSWORD));
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
                Class<? extends Throwable> outcome = result.get();
                if (outcome == null) {
                    successes++;
                } else if (outcome == InvalidTokenException.class) {
                    failures++;
                }
            }
            assertEquals(1, successes);
            assertEquals(1, failures);
        }
    }

    private String requestResetToken() {
        AtomicReference<String> token = new AtomicReference<>();
        doAnswer(invocation -> {
            token.set(invocation.getArgument(1));
            return null;
        }).when(passwordResetEmailService)
                .sendPasswordResetEmail(eq(EMAIL), org.mockito.ArgumentMatchers.anyString());

        authService.forgotPassword(new ForgotPasswordRequest(EMAIL));
        assertNotNull(token.get());
        return token.get();
    }

    private String jwtFor(String userId) {
        return jwtService.generatePasswordResetToken(userId);
    }
}
