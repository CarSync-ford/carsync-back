package br.com.sprint1.challenge.repository;

import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.entity.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTypeRepository userTypeRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userTypeRepository.deleteAll();

        UserType userType = new UserType("a1b2c3d4-e5f6-7890-abcd-ef1234567890", "USER");
        userTypeRepository.save(userType);

        User user = new User();
        user.setId("test-user-id");
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setHashedPassword("$2a$10$hashed");
        user.setCpf("12345678901");
        user.setUserType(userType);
        user.setMfaEnabled(false);
        userRepository.save(user);
    }

    @Test
    void findByEmail_quandoExiste_retornaUsuario() {
        Optional<User> result = userRepository.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void findByEmail_quandoInexiste_retornaEmpty() {
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        assertTrue(result.isEmpty());
    }
}