package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.config.JwtProperties;
import br.com.sprint1.challenge.dto.AuthDtos.AuthRequest;
import br.com.sprint1.challenge.dto.AuthDtos.AuthResponse;
import br.com.sprint1.challenge.dto.AuthDtos.ChangePasswordRequest;
import br.com.sprint1.challenge.dto.AuthDtos.ForgotPasswordRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // When
        assertDoesNotThrow(() -> authService.forgotPassword(request));

        // Then
        verify(jwtService, never()).generatePasswordResetToken(any());
    }

    @Test
    void forgotPassword_usuarioExiste_geraToken() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest(TEST_EMAIL);
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generatePasswordResetToken(TEST_USER_ID)).thenReturn("reset-token");

        // When
        assertDoesNotThrow(() -> authService.forgotPassword(request));

        // Then
        verify(jwtService).generatePasswordResetToken(TEST_USER_ID);
    }

    // --- Reset Password Tests ---

    @Test
    void resetPassword_tokenValido_atualizaSenhaERevogaRefreshToken() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "newpassword123");
        var claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn(TEST_USER_ID);
        when(jwtService.parsePasswordResetToken("reset-token")).thenReturn(claims);

        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

        // When
        assertDoesNotThrow(() -> authService.resetPassword(request));

        // Then
        verify(userRepository).save(user);
        verify(userRepository).revokeRefreshToken(TEST_USER_ID);
        assertTrue(BCrypt.checkpw("newpassword123", user.getHashedPassword()));
    }

    @Test
    void resetPassword_usuarioNaoExiste_lancaExcecao() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "newpassword123");
        var claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn(TEST_USER_ID);
        when(jwtService.parsePasswordResetToken("reset-token")).thenReturn(claims);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // When/Then
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

        when(userRepository.findByIdForUpdate(TEST_USER_ID)).thenReturn(Optional.of(user));

        ChangePasswordRequest request = new ChangePasswordRequest(TEST_PASSWORD, "newpassword123");

        // When
        assertDoesNotThrow(() -> authService.changePassword(TEST_USER_ID, request));

        // Then
        verify(userRepository).save(user);
        verify(userRepository).revokeRefreshToken(TEST_USER_ID);
        assertTrue(BCrypt.checkpw("newpassword123", user.getHashedPassword()));
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

        ChangePasswordRequest request = new ChangePasswordRequest("wrongpassword", "newpassword123");

        // When/Then
        assertThrows(InvalidCredentialsException.class, () -> authService.changePassword(TEST_USER_ID, request));
    }

    // --- MFA Tests ---

    @Test
    void enableMfa_retornaSecretEQrCode() {
        // Given
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

        // When
        MfaEnableResponse response = authService.enableMfa(TEST_USER_ID);

        // Then
        assertNotNull(response);
        assertNotNull(response.secret());
        assertNotNull(response.qrCodeUri());
        assertTrue(response.qrCodeUri().contains("otpauth://totp/"));
        assertTrue(response.qrCodeUri().contains(TEST_EMAIL));
        verify(userRepository).save(user);
        assertNotNull(user.getMfaSecret());
    }

    @Test
    void verifyMfa_codigoValido_ativaMfa() {
        // Given
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        String secret = "JBSWY3DPEHPK3PXP"; // valid base32
        user.setMfaSecret(secret);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

        // Generate a valid TOTP code for current time window
        String validCode = generateValidTotpCode(secret);

        MfaVerifyRequest request = new MfaVerifyRequest(validCode);

        // When
        assertDoesNotThrow(() -> authService.verifyMfa(TEST_USER_ID, request));

        // Then
        verify(userRepository).save(user);
        assertTrue(user.getMfaEnabled());
    }

    @Test
    void verifyMfa_codigoInvalido_lancaExcecao() {
        // Given
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setMfaSecret("JBSWY3DPEHPK3PXP");

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

        MfaVerifyRequest request = new MfaVerifyRequest("000000");

        // When/Then
        assertThrows(InvalidCredentialsException.class, () -> authService.verifyMfa(TEST_USER_ID, request));
    }

    @Test
    void verifyMfa_mfaNaoHabilitado_lancaExcecao() {
        // Given
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setMfaSecret(null);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

        MfaVerifyRequest request = new MfaVerifyRequest("123456");

        // When/Then
        assertThrows(InvalidTokenException.class, () -> authService.verifyMfa(TEST_USER_ID, request));
    }

    @Test
    void disableMfa_desativaMfaELimpaSecret() {
        // Given
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setMfaEnabled(true);
        user.setMfaSecret("JBSWY3DPEHPK3PXP");

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

        // When
        assertDoesNotThrow(() -> authService.disableMfa(TEST_USER_ID));

        // Then
        verify(userRepository).save(user);
        assertFalse(user.getMfaEnabled());
        assertNull(user.getMfaSecret());
    }

    // Helper to generate valid TOTP code
    private String generateValidTotpCode(String secret) {
        try {
            long timeWindow = System.currentTimeMillis() / 1000 / 30;
            byte[] key = base32Decode(secret);
            byte[] data = new byte[8];
            long tw = timeWindow;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (tw & 0xFF);
                tw >>= 8;
            }

            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0xF;
            int truncatedHash = 0;
            for (int i = 0; i < 4; i++) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }

            truncatedHash &= 0x7FFFFFFF;
            int totp = truncatedHash % 1000000;
            return String.format("%06d", totp);
        } catch (Exception e) {
            return "000000";
        }
    }

    private byte[] base32Decode(String encoded) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int bits = encoded.length() * 5;
        byte[] result = new byte[bits / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : encoded.toCharArray()) {
            int val = alphabet.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }
}