package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.UserDtos.CreateUserRequest;
import br.com.sprint1.challenge.dto.UserDtos.UserCreatedResponse;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.entity.UserType;
import br.com.sprint1.challenge.exception.DuplicateCpfException;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.repository.UserTypeRepository;
import br.com.sprint1.challenge.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTypeRepository userTypeRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userTypeRepository, 10);
    }

    @Test
    void criacaoComSucesso_retornaUuidSenhaHashMfaFalse() {
        // Given
        CreateUserRequest request = new CreateUserRequest(
                "johndoe",
                "john@example.com",
                "Password@1",
                "52998224725"
        );
        UserType userType = new UserType("a1b2c3d4-e5f6-7890-abcd-ef1234567890", "USER");
        
        when(userRepository.existsByCpf("52998224725")).thenReturn(false);
        when(userTypeRepository.findByType("USER")).thenReturn(Optional.of(userType));
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("test-uuid");
            return u;
        });

        // When
        UserCreatedResponse response = userService.create(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.id());
        verify(userRepository).save(argThat(user -> 
                user.getMfaEnabled() == false &&
                !user.getHashedPassword().equals("Password@1") // password is hashed
        ));
    }

    @Test
    void cpfDuplicado_lancaDuplicateCpfException() {
        // Given
        CreateUserRequest request = new CreateUserRequest(
                "johndoe",
                "john@example.com",
                "Password@1",
                "52998224725"
        );
        when(userRepository.existsByCpf("52998224725")).thenReturn(true);

        // When/Then
        assertThrows(DuplicateCpfException.class, () -> userService.create(request));
        verify(userRepository, never()).save(any());
    }
}