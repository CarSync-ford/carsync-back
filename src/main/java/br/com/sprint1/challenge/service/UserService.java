package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.dto.UserDtos.CreateUserRequest;
import br.com.sprint1.challenge.dto.UserDtos.GetUserResponse;
import br.com.sprint1.challenge.dto.UserDtos.UserCreatedResponse;

public interface UserService {
    UserCreatedResponse create(CreateUserRequest request);
    GetUserResponse getById(String userId);
}