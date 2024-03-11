package com.kasumov.WebfluxRestApp.rest;

import com.kasumov.WebfluxRestApp.dto.AuthRequestDTO;
import com.kasumov.WebfluxRestApp.dto.AuthResponseDTO;
import com.kasumov.WebfluxRestApp.dto.UserDTO;
import com.kasumov.WebfluxRestApp.dto.UserRequestDTO;
import com.kasumov.WebfluxRestApp.mapper.UserMapper;
import com.kasumov.WebfluxRestApp.security.CustomPrincipal;
import com.kasumov.WebfluxRestApp.security.SecurityService;
import com.kasumov.WebfluxRestApp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Operations related to auth")
public class AuthRestControllerV1 {

    private final SecurityService securityService;
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Register a new user with role USER by default")
    public Mono<UserDTO> register(@RequestBody UserRequestDTO userRequestDTO) {
        return userService.registerUser(userRequestDTO)
                .map(userMapper::mapToUserDTO);
    }

    @PostMapping("/login")
    @Operation(summary = "Login a user", description = "Login a user by username and password")
    public Mono<AuthResponseDTO> login(@RequestBody AuthRequestDTO dto) {
        return securityService.authenticate(dto.getUsername(), dto.getPassword())
                .flatMap(tokenDetails -> Mono.just(
                        AuthResponseDTO.builder()
                                .userId(tokenDetails.getUserId())
                                .token(tokenDetails.getToken())
                                .issuedAt(tokenDetails.getIssuedAt())
                                .expiresAt(tokenDetails.getExpiresAt())
                                .build()
                ));
    }

    @GetMapping("/info")
    @Operation(summary = "Get user details", description = "Get user details by JWT token")
    public Mono<UserDTO> getUserInfo(Authentication authentication) {
        CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();

        return userService.getUserById(customPrincipal.getId())
                .map(userMapper::mapToUserDTO);
    }
}