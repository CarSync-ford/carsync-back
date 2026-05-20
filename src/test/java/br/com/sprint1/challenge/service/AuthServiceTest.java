package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.AuthDtos.AuthRequest;
import br.com.sprint1.challenge.dto.AuthDtos.AuthResponse;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.exception.InvalidCredentialsException;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthServiceImpl(userRepository, jwtService, 10);
        // Manually invoke @PostConstruct init()
        var initMethod = AuthServiceImpl.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(authService);
    }

    @Test
    void usuarioNaoExistente_lancaExcecaoGenerica() {
        // Given
        AuthRequest request = new AuthRequest(TEST_EMAIL, TEST_PASSWORD);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

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
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));

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
        
        String expectedToken = "jwt-token";
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(TEST_USER_ID, TEST_EMAIL, "USER")).thenReturn(expectedToken);

        // When
        AuthResponse response = authService.authenticate(request);

        // Then
        assertNotNull(response);
        assertEquals(expectedToken, response.token());
        verify(userRepository).updateLastLoginById(TEST_USER_ID);
    }
}