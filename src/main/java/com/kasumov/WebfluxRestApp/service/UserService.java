package com.kasumov.WebfluxRestApp.service;

import com.kasumov.WebfluxRestApp.dto.UserDTO;
import com.kasumov.WebfluxRestApp.dto.UserRequestDTO;
import com.kasumov.WebfluxRestApp.model.UserEntity;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<UserEntity> registerUser(UserRequestDTO user);
    Mono<UserEntity> getUserByUsername(String username);
    Mono<UserEntity> getUserById(Long id);

    Flux<UserDTO> getAllUsers();

    Mono<UserDTO> updateUserById(Long id, UserRequestDTO userUpdateRequestDTO);

    Mono<Void> deleteUserById(Long id);

    Mono<Integer> deleteAllUsers();

    Mono<UserDTO> getUserByIdAndAuth(Long id, Mono<Authentication> authMono);
}
