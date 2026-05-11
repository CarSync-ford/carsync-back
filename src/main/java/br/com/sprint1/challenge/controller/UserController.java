package br.com.sprint1.challenge.controller;

import br.com.sprint1.challenge.dto.UserDtos.CreateUserRequest;
import br.com.sprint1.challenge.dto.UserDtos.UserCreatedResponse;
import br.com.sprint1.challenge.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/user", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
    })
    @ResponseStatus(HttpStatus.CREATED)
    public UserCreatedResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }
}