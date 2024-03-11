package com.kasumov.WebfluxRestApp.service.impl;

import com.kasumov.WebfluxRestApp.dto.UserDTO;
import com.kasumov.WebfluxRestApp.dto.UserRequestDTO;
import com.kasumov.WebfluxRestApp.mapper.EventMapper;
import com.kasumov.WebfluxRestApp.mapper.UserMapper;
import com.kasumov.WebfluxRestApp.model.File;
import com.kasumov.WebfluxRestApp.model.UserEntity;
import com.kasumov.WebfluxRestApp.model.UserRole;
import com.kasumov.WebfluxRestApp.repository.EventRepository;
import com.kasumov.WebfluxRestApp.repository.FileRepository;
import com.kasumov.WebfluxRestApp.repository.UserRepository;
import com.kasumov.WebfluxRestApp.security.CustomPrincipal;
import com.kasumov.WebfluxRestApp.service.UserService;
import com.kasumov.WebfluxRestApp.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final FileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EventMapper eventMapper;

    @Override
    public Mono<UserEntity> registerUser(UserRequestDTO userRequestDTO) {
        log.info("RegisterUser: {}", userRequestDTO);
        UserEntity user = userMapper.map(userRequestDTO);
        return userRepository.save(
                user.toBuilder()
                        .password(passwordEncoder.encode(userRequestDTO.getPassword()))
                        .role(UserRole.USER)
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .build()
        ).doOnSuccess(u -> log.info("USER_CREATED", u));
    }

    @Override
    public Mono<UserEntity> getUserById(Long id) {
        log.info("GetUserById: {}", id);
        return userRepository.findActiveById(id)
                .doOnSuccess(aVoid -> log.info(String.valueOf(id)))
                .doOnError(error -> log.error(error.getMessage(), id))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(String.valueOf(id)))));
    }

    @Override
    public Mono<UserDTO> getUserByIdAndAuth(Long id, Mono<Authentication> authMono) {
        log.info("GetUserByIdAndAuth: {}", id);
        return authMono
                .flatMap(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return SecurityUtils.isAdminOrModerator(Mono.just(authentication))
                            .flatMap(isAdminOrModerator -> {
                                if (isAdminOrModerator || principal.getId().equals(id)) {
                                    return userRepository.findActiveById(id);
                                } else {
                                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                                }
                            });
                })
                .flatMap(userEntity ->
                        eventRepository.findAllActiveByUserId(userEntity.getId())
                                .flatMap(event ->
                                        fileRepository.findActiveById(event.getFileId())
                                                .defaultIfEmpty(new File())
                                                .map(file -> eventMapper.map(event, file))
                                )
                                .collectList()
                                .map(eventDTOs -> {
                                    UserDTO userDTO = userMapper.map(userEntity);
                                    userDTO.setEventDTOs(eventDTOs);
                                    return userDTO;
                                })
                )
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(String.valueOf(id)))))
                .doOnSuccess(unused -> log.info(String.valueOf(id)))
                .doOnError(error -> log.error(error.getMessage(), id));
    }

    @Override
    public Mono<UserEntity> getUserByUsername(String username) {
        log.info("GetUserByUsername {}", username);
        return userRepository.findActiveByUsername(username)
                .doOnSuccess(aVoid -> log.info("FOUND_SUCCESSFULLY_USER_WITH_USERNAME", username))
                .doOnError(error -> log.error(username, error.getMessage()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(username))));
    }

    @Override
    public Flux<UserDTO> getAllUsers() {
        log.info("GetAllUsers");
        return userRepository.findAllActive()
                .map(userMapper::mapToUserDTO);
    }

    @Override
    public Mono<UserDTO> updateUserById(Long id, UserRequestDTO userRequestDTO) {
        log.info("UpdateUserById {}: {}", id, userRequestDTO);
        return userRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, String.format(String.valueOf(id)))))
                .flatMap(existingUser -> userRepository.existsByUsernameAndIdNot(existingUser.getUsername(), id)
                        .flatMap(exists -> {
                            if (Boolean.TRUE.equals(exists)) {
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
                            }
                            userMapper.updateUserEntityFromUserRequestDTO(userRequestDTO, existingUser);
                            existingUser.setId(id);
                            if (userRequestDTO.getPassword() != null && !userRequestDTO.getPassword().isEmpty()) {
                                existingUser.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
                            }
                            existingUser.setUpdatedAt(LocalDateTime.now());
                            return userRepository.save(existingUser);
                        }))
                .map(userMapper::mapToUserDTO)
                .doOnSuccess(aVoid -> log.info("UPDATED_SUCCESSFULLY_WITH_ID", id))
                .doOnError(error -> log.error(error.getMessage(), userRequestDTO));
    }

    @Override
    public Mono<Void> deleteUserById(Long id) {
        log.info("DeleteUserById '{}'", id);
        return userRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(String.valueOf(id)))))
                .flatMap(user -> {
                    log.info(String.valueOf(id));
                    return userRepository.deleteActiveById(id);
                })
                .then()
                .doOnSuccess(aVoid -> log.info("DELETED_SUCCESSFULLY_WITH_ID", id))
                .doOnError(error -> log.error(error.getMessage(), id));
    }

    @Override
    public Mono<Integer> deleteAllUsers() {
        log.info("DeleteAllUsers");
        return userRepository.deleteAllActive()
                .doOnTerminate(() -> log.info("DELETED_SUCCESSFULLY"))
                .doOnError(error -> log.error(error.getMessage()));
    }
}
