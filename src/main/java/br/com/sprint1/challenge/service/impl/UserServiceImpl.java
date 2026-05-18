package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.dto.UserDtos.CreateUserRequest;
import br.com.sprint1.challenge.dto.UserDtos.GetUserResponse;
import br.com.sprint1.challenge.dto.UserDtos.UserCreatedResponse;
import br.com.sprint1.challenge.entity.User;
import br.com.sprint1.challenge.entity.UserType;
import br.com.sprint1.challenge.exception.DuplicateCpfException;
import br.com.sprint1.challenge.exception.DuplicateEmailException;
import br.com.sprint1.challenge.exception.ResourceNotFoundException;
import br.com.sprint1.challenge.repository.UserRepository;
import br.com.sprint1.challenge.repository.UserTypeRepository;
import br.com.sprint1.challenge.service.UserService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final int bcryptRounds;

    public UserServiceImpl(
            UserRepository userRepository,
            UserTypeRepository userTypeRepository,
            @Value("${spring.bcrypt.salt:10}") int bcryptRounds) {
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.bcryptRounds = bcryptRounds;
    }

    @Override
    public UserCreatedResponse create(CreateUserRequest request) {
        if (userRepository.existsByCpf(request.cpf())) {
            throw new DuplicateCpfException();
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setHashedPassword(BCrypt.hashpw(request.password(), BCrypt.gensalt(bcryptRounds)));
        user.setCpf(request.cpf());
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setLastLogin(null);
        user.setRefreshToken(null);

        Optional<UserType> userType = userTypeRepository.findByType("USER");
        userType.ifPresent(user::setUserType);

        User saved = userRepository.save(user);
        return new UserCreatedResponse(saved.getId());
    }

    @Override
    public GetUserResponse getById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new GetUserResponse(user.getUsername());
    }
}