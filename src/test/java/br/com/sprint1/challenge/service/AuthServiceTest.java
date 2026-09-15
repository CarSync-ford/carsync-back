package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.config.JwtProperties;
import br.com.sprint1.challenge.dto.AuthDtos.AuthRequest;
import br.com.sprint1.challenge.dto.AuthDtos.AuthResponse;
import br.com.sprint1.challenge.dto.AuthDtos.ChangePasswordRequest;
import br.com.sprint1.challenge.dto.AuthDtos.ForgotPasswordRequest;
import br.com.sprint1.challenge.dto.AuthDtos.MfaDisableRequest;
import br.com.sprint1.challenge.dto.AuthDtos.MfaEnableResponse;
import br.com.sprint1.challenge.dto.AuthDtos.MfaVerifyRequest;
import br.com.sprint1.challenge.dto.AuthDtos.RefreshTokenRequest;
import br.com.sprint1.challenge.dto.AuthDtos.RefreshTokenResponse;
import br.com.sprint1.challenge.dto.AuthDtos.ResetPasswordRequest;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.entity.UserType;
import br.com.sprint1.challenge.exception.InvalidCredentialsException;
import br.com.sprint1.challenge.exception.InvalidTokenException;
import br.com.sprint1.challenge.exception.TokenExpiredException;
import br.com.sprint1.challenge.exception.UserLockedException;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.service.impl.AuthServiceImpl;
import br.com.sprint1.challenge.util.TotpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordResetEmailService passwordResetEmailService;

    private AuthService authService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_USER_ID = "user-uuid";
    private static final String TEST_REFRESH_TOKEN = "refresh-token";
    private static final String TEST_ACCESS_TOKEN = "access-token";

    @BeforeEach
    void setUp() throws Exception {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenExpiryDays(30);
        authService = new AuthServiceImpl(
                userRepository,
                jwtService,
                passwordResetEmailService,
                jwtProperties,
                Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC),
                10);
        // Manually invoke @PostConstruct init()
        var initMethod = AuthServiceImpl.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(authService);
    }

    // --- Login Tests ---

    @Test
    void usuarioNaoExistente_lancaExcecaoGenerica() {
        // Given
        AuthRequest request = new AuthRequest(TEST_EMAIL, TEST_PASSWORD);
        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(request));
        verify(userRepository, never()).updateLastLoginById(any());
    }

    @Test
    void senhaInvalida_lancaExcecaoGenerica() {
        // Given
        AuthRequest request = new AuthRequest(TEST_EMAIL, "wrongpassword");
        String hashedPassword = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10));

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(hashedPassword);

        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.of(user));

        // When/Then
        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(request));
        verify(userRepository, never()).updateLastLoginById(any());
    }

    @Test
    void credenciaisValidas_retornaTokenEAtualizaLastLogin() {
        // Given
        AuthRequest request = new AuthRequest(TEST_EMAIL, TEST_PASSWORD);
        String hashedPassword = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10));

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(hashedPassword);
        user.setUserType(new UserType());
        user.getUserType().setType("USER");

        String expectedToken = "jwt-token";
        String expectedRefreshToken = "refresh-token";

        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(TEST_USER_ID, TEST_EMAIL, "USER")).thenReturn(expectedToken);
        when(jwtService.generateRefreshToken(TEST_USER_ID)).thenReturn(expectedRefreshToken);

        // When
        AuthResponse response = authService.authenticate(request);

        // Then
        assertNotNull(response);
        assertEquals(expectedToken, response.token());
        assertEquals(expectedRefreshToken, response.refreshToken());
        assertEquals(LocalDateTime.of(2026, 9, 15, 12, 0), user.getLastLogin());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
        verify(userRepository).updateRefreshToken(eq(TEST_USER_ID), eq(expectedRefreshToken), any());
    }

    @Test
    void contaBloqueada_lancaUserLockedException() {
        // Given
        AuthRequest request = new AuthRequest(TEST_EMAIL, TEST_PASSWORD);
        String hashedPassword = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10));

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(hashedPassword);
        user.setLockedUntil(LocalDateTime.of(2026, 9, 15, 12, 10));

        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.of(user));

        // When/Then
        UserLockedException ex = assertThrows(UserLockedException.class, () -> authService.authenticate(request));
        assertNotNull(ex.getLockedUntil());
        verify(userRepository, never()).updateLastLoginById(any());
    }

    @Test
    void contaDesbloqueada_aposTempoExpirado() {
        // Given
        AuthRequest request = new AuthRequest(TEST_EMAIL, TEST_PASSWORD);
        String hashedPassword = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10));

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(hashedPassword);
        user.setLockedUntil(LocalDateTime.of(2026, 9, 15, 11, 50)); // expired lock
        user.setFailedLoginAttempts(5);

        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(TEST_USER_ID, TEST_EMAIL, "USER")).thenReturn(TEST_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_USER_ID)).thenReturn(TEST_REFRESH_TOKEN);

        // When
        AuthResponse response = authService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNull(user.getLockedUntil());
        assertEquals(0, user.getFailedLoginAttempts());
    }

    @Test
    void falhaLogin_incrementaTentativas() {
        // Given
        AuthRequest request = new AuthRequest(TEST_EMAIL, "wrongpassword");
        String hashedPassword = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10));

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(hashedPassword);
        user.setFailedLoginAttempts(0);

        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.of(user));

        // When/Then
        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(request));
        assertEquals(1, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }

    @Test
    void cincoFalhas_contaBloqueada() {
        // Given
        AuthRequest request = new AuthRequest(TEST_EMAIL, "wrongpassword");
        String hashedPassword = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10));

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(hashedPassword);
        user.setFailedLoginAttempts(4); // 4 attempts, next will be 5th

        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.of(user));

        // When/Then
        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(request));
        assertEquals(5, user.getFailedLoginAttempts());
        assertEquals(LocalDateTime.of(2026, 9, 15, 12, 15), user.getLockedUntil());
    }

    // --- Refresh Token Tests ---

    @Test
    void refreshToken_valido_retornaNovosTokens() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest(TEST_REFRESH_TOKEN);
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setRefreshToken(TEST_REFRESH_TOKEN);
        user.setRefreshTokenExpiresAt(LocalDateTime.of(2026, 9, 25, 12, 0));
        user.setUserType(new UserType());
        user.getUserType().setType("USER");

        var claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn(TEST_USER_ID);
        when(claims.get("type", String.class)).thenReturn("REFRESH");
        when(jwtService.parse(TEST_REFRESH_TOKEN)).thenReturn(claims);
        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(TEST_USER_ID, TEST_EMAIL, "USER")).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(TEST_USER_ID)).thenReturn("new-refresh-token");

        // When
        RefreshTokenResponse response = authService.refreshToken(request);

        // Then
        assertNotNull(response);
        assertEquals("new-access-token", response.token());
        assertEquals("new-refresh-token", response.refreshToken());
        verify(userRepository).updateRefreshToken(eq(TEST_USER_ID), eq("new-refresh-token"), any());
    }

    @Test
    void refreshToken_invalido_lancaExcecao() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        var claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn(TEST_USER_ID);
        when(claims.get("type", String.class)).thenReturn("REFRESH");
        when(jwtService.parse("invalid-token")).thenReturn(claims);
        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_expirado_lancaExcecao() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest(TEST_REFRESH_TOKEN);
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setRefreshToken(TEST_REFRESH_TOKEN);
        user.setRefreshTokenExpiresAt(LocalDateTime.of(2026, 9, 14, 12, 0));

        var claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn(TEST_USER_ID);
        when(claims.get("type", String.class)).thenReturn("REFRESH");
        when(jwtService.parse(TEST_REFRESH_TOKEN)).thenReturn(claims);
        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        // When/Then
        assertThrows(TokenExpiredException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_tipoErrado_lancaExcecao() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest(TEST_REFRESH_TOKEN);

        var claims = mock(io.jsonwebtoken.Claims.class);
        lenient().when(claims.getSubject()).thenReturn(TEST_USER_ID);
        lenient().when(claims.get("type", String.class)).thenReturn("ACCESS"); // wrong type
        when(jwtService.parse(TEST_REFRESH_TOKEN)).thenReturn(claims);

        // When/Then
        // Note: Exception is thrown before userRepository.findById is called
        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(request));
    }

    // --- Forgot Password Tests ---

    @Test
    void forgotPassword_usuarioNaoExistente_retorna202() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest(TEST_EMAIL);
        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.empty());

        // When
        assertDoesNotThrow(() -> authService.forgotPassword(request));

        // Then
        verify(jwtService, never()).generatePasswordResetToken(any());
    }

    @Test
    void forgotPassword_usuarioExiste_persisteHashEEnviaToken() {
        ForgotPasswordRequest request = new ForgotPasswordRequest(TEST_EMAIL);
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);

        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generatePasswordResetToken(TEST_USER_ID)).thenReturn("reset-token");

        assertDoesNotThrow(() -> authService.forgotPassword(request));

        verify(passwordResetEmailService).sendPasswordResetEmail(TEST_EMAIL, "reset-token");
        assertEquals(64, user.getPasswordResetTokenHash().length());
        assertNotEquals("reset-token", user.getPasswordResetTokenHash());
        assertEquals(LocalDateTime.of(2026, 9, 15, 12, 15), user.getPasswordResetTokenExpiresAt());
    }

    // --- Reset Password Tests ---

    @Test
    void resetPassword_tokenValido_atualizaSenhaConsomeTokenERevogaRefreshToken() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "NewPassword1!");
        var claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn(TEST_USER_ID);
        when(claims.getExpiration()).thenReturn(java.util.Date.from(Instant.parse("2026-09-15T12:15:00Z")));
        when(jwtService.parsePasswordResetToken("reset-token")).thenReturn(claims);

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setPasswordResetTokenHash(hashToken("reset-token"));
        user.setPasswordResetTokenExpiresAt(LocalDateTime.of(2026, 9, 15, 12, 15));
        user.setRefreshToken(TEST_REFRESH_TOKEN);
        user.setRefreshTokenExpiresAt(LocalDateTime.of(2026, 10, 15, 12, 0));
        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> authService.resetPassword(request));

        assertTrue(BCrypt.checkpw("NewPassword1!", user.getHashedPassword()));
        assertNull(user.getPasswordResetTokenHash());
        assertNull(user.getPasswordResetTokenExpiresAt());
        assertNull(user.getRefreshToken());
        assertNull(user.getRefreshTokenExpiresAt());
    }

    @Test
    void resetPassword_usuarioNaoExiste_lancaExcecao() {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "NewPassword1!");
        var claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn(TEST_USER_ID);
        when(jwtService.parsePasswordResetToken("reset-token")).thenReturn(claims);
        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> authService.resetPassword(request));
    }

    // --- Change Password Tests ---

    @Test
    void changePassword_senhaAtualCorreta_atualizaSenhaERevogaRefreshToken() {
        // Given
        String hashedPassword = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10));
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(hashedPassword);
        user.setPasswordResetTokenHash("pending-reset-hash");
        user.setPasswordResetTokenExpiresAt(LocalDateTime.of(2026, 9, 15, 12, 15));
        user.setRefreshToken(TEST_REFRESH_TOKEN);
        user.setRefreshTokenExpiresAt(LocalDateTime.of(2026, 10, 15, 12, 0));

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        ChangePasswordRequest request = new ChangePasswordRequest(TEST_PASSWORD, "NewPassword1!");

        // When
        assertDoesNotThrow(() -> authService.changePassword(TEST_USER_ID, request));

        // Then
        assertTrue(BCrypt.checkpw("NewPassword1!", user.getHashedPassword()));
        assertNull(user.getPasswordResetTokenHash());
        assertNull(user.getPasswordResetTokenExpiresAt());
        assertNull(user.getRefreshToken());
        assertNull(user.getRefreshTokenExpiresAt());
    }

    @Test
    void changePassword_senhaAtualIncorreta_lancaExcecao() {
        // Given
        String hashedPassword = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10));
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(hashedPassword);

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        ChangePasswordRequest request = new ChangePasswordRequest("wrongpassword", "NewPassword1!");

        // When/Then
        assertThrows(InvalidCredentialsException.class, () -> authService.changePassword(TEST_USER_ID, request));
    }

    // --- MFA Tests ---

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC);
    private static final long CURRENT_STEP = Instant.parse("2026-09-15T12:00:00Z").getEpochSecond() / 30;
    private static final String MFA_SECRET = "JBSWY3DPEHPK3PXP";

    private String codeForStep(long step) {
        return TotpUtil.generateCode(TotpUtil.base32Decode(MFA_SECRET), step);
    }

    @Test
    void enableMfa_retornaSecretEQrCode() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        MfaEnableResponse response = authService.enableMfa(TEST_USER_ID);

        assertNotNull(response);
        assertNotNull(response.secret());
        assertTrue(response.qrCodeUri().contains("otpauth://totp/"));
        assertTrue(response.qrCodeUri().contains(TEST_EMAIL));
        assertNotNull(user.getMfaSecret());
        assertFalse(user.getMfaEnabled());
    }

    @Test
    void enableMfa_mfaJaAtivo_lancaExcecao() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setMfaEnabled(true);
        user.setMfaSecret(MFA_SECRET);

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> authService.enableMfa(TEST_USER_ID));
        assertEquals(MFA_SECRET, user.getMfaSecret()); // not overwritten
    }

    @Test
    void verifyMfa_codigoValido_ativaMfaERevogaRefresh() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setMfaSecret(MFA_SECRET);
        user.setRefreshToken("old-refresh");
        user.setRefreshTokenExpiresAt(LocalDateTime.of(2026, 10, 1, 0, 0));

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        String validCode = codeForStep(CURRENT_STEP);
        assertDoesNotThrow(() -> authService.verifyMfa(TEST_USER_ID, new MfaVerifyRequest(validCode)));

        assertTrue(user.getMfaEnabled());
        assertEquals(CURRENT_STEP, user.getMfaLastUsedStep());
        assertNull(user.getRefreshToken());
        assertNull(user.getRefreshTokenExpiresAt());
    }

    @Test
    void verifyMfa_codigoInvalido_lancaExcecaoEIncrementaFalhas() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setMfaSecret(MFA_SECRET);
        user.setFailedLoginAttempts(0);

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.verifyMfa(TEST_USER_ID, new MfaVerifyRequest("000000")));
        assertEquals(1, user.getFailedLoginAttempts());
    }

    @Test
    void verifyMfa_semSecret_lancaExcecao() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setMfaSecret(null);

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        assertThrows(InvalidTokenException.class,
                () -> authService.verifyMfa(TEST_USER_ID, new MfaVerifyRequest("123456")));
    }

    @Test
    void disableMfa_credenciaisValidas_limpaMfaERevogaRefresh() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10)));
        user.setMfaEnabled(true);
        user.setMfaSecret(MFA_SECRET);
        user.setRefreshToken("old-refresh");
        user.setRefreshTokenExpiresAt(LocalDateTime.of(2026, 10, 1, 0, 0));

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        String validCode = codeForStep(CURRENT_STEP);
        MfaDisableRequest request = new MfaDisableRequest(TEST_PASSWORD, validCode);

        assertDoesNotThrow(() -> authService.disableMfa(TEST_USER_ID, request));

        assertFalse(user.getMfaEnabled());
        assertNull(user.getMfaSecret());
        assertNull(user.getMfaLastUsedStep());
        assertNull(user.getRefreshToken());
        assertNull(user.getRefreshTokenExpiresAt());
    }

    @Test
    void disableMfa_senhaErrada_lancaExcecao() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10)));
        user.setMfaEnabled(true);
        user.setMfaSecret(MFA_SECRET);
        user.setFailedLoginAttempts(0);

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        MfaDisableRequest request = new MfaDisableRequest("wrongpassword", codeForStep(CURRENT_STEP));
        assertThrows(InvalidCredentialsException.class, () -> authService.disableMfa(TEST_USER_ID, request));
        assertEquals(1, user.getFailedLoginAttempts());
    }

    @Test
    void disableMfa_codigoErrado_lancaExcecao() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10)));
        user.setMfaEnabled(true);
        user.setMfaSecret(MFA_SECRET);
        user.setFailedLoginAttempts(0);

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        MfaDisableRequest request = new MfaDisableRequest(TEST_PASSWORD, "000000");
        assertThrows(InvalidCredentialsException.class, () -> authService.disableMfa(TEST_USER_ID, request));
        assertEquals(1, user.getFailedLoginAttempts());
    }

    @Test
    void disableMfa_mfaNaoAtivo_lancaExcecao() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setMfaEnabled(false);

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        MfaDisableRequest request = new MfaDisableRequest(TEST_PASSWORD, "123456");
        assertThrows(InvalidTokenException.class, () -> authService.disableMfa(TEST_USER_ID, request));
    }

    @Test
    void loginComMfa_semCodigo_falha() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10)));
        user.setMfaEnabled(true);
        user.setMfaSecret(MFA_SECRET);

        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.of(user));

        AuthRequest request = new AuthRequest(TEST_EMAIL, TEST_PASSWORD);
        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(request));
    }

    @Test
    void loginComMfa_codigoValido_retornaTokens() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10)));
        user.setMfaEnabled(true);
        user.setMfaSecret(MFA_SECRET);
        user.setUserType(new UserType());
        user.getUserType().setType("USER");

        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(TEST_USER_ID, TEST_EMAIL, "USER")).thenReturn(TEST_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_USER_ID)).thenReturn(TEST_REFRESH_TOKEN);

        String code = codeForStep(CURRENT_STEP);
        AuthRequest request = new AuthRequest(TEST_EMAIL, TEST_PASSWORD, code);
        AuthResponse response = authService.authenticate(request);

        assertNotNull(response);
        assertEquals(CURRENT_STEP, user.getMfaLastUsedStep());
    }

    @Test
    void loginComMfa_codigoErrado_incrementaFalhas() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10)));
        user.setMfaEnabled(true);
        user.setMfaSecret(MFA_SECRET);
        user.setFailedLoginAttempts(0);

        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.of(user));

        AuthRequest request = new AuthRequest(TEST_EMAIL, TEST_PASSWORD, "000000");
        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(request));
        assertEquals(1, user.getFailedLoginAttempts());
    }

    @Test
    void loginComMfa_replayMesmoStep_falha() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setHashedPassword(BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt(10)));
        user.setMfaEnabled(true);
        user.setMfaSecret(MFA_SECRET);
        user.setMfaLastUsedStep(CURRENT_STEP); // already used
        user.setUserType(new UserType());
        user.getUserType().setType("USER");

        when(userRepository.findByEmailForUpdate(TEST_EMAIL)).thenReturn(Optional.of(user));

        String code = codeForStep(CURRENT_STEP);
        AuthRequest request = new AuthRequest(TEST_EMAIL, TEST_PASSWORD, code);
        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(request));
    }

    private String hashToken(String token) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(token.getBytes(StandardCharsets.UTF_8)));
    }
}