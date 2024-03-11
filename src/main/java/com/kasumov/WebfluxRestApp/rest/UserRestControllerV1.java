package com.kasumov.WebfluxRestApp.rest;

import com.kasumov.WebfluxRestApp.dto.UserDTO;
import com.kasumov.WebfluxRestApp.dto.UserRequestDTO;
import com.kasumov.WebfluxRestApp.security.CustomPrincipal;
import com.kasumov.WebfluxRestApp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "Operations related to users")
public class UserRestControllerV1 {

    private final UserService userService;

    @GetMapping("/{id}")
    @Operation(summary = "Find a user by ID", description = "Finds a user with the specified ID (if role USER access to own data only)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'USER')")
    public Mono<UserDTO> getUserByIdAndAuth(@PathVariable Long id, Mono<Authentication> authMono) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (CustomPrincipal) securityContext.getAuthentication().getPrincipal())
                .flatMap(customPrincipal ->
                        userService.getUserByIdAndAuth(id, authMono));
    }

    @GetMapping("/")
    @Operation(summary = "Find all users", description = "Finds all users")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public Flux<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user by user ID", description = "Updates a user by ID and request body")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Mono<UserDTO> updateUserById(@PathVariable Long id, @RequestBody UserRequestDTO userRequestDTO) {
        return userService.updateUserById(id, userRequestDTO);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user by ID", description = "Deletes a user with the specified ID")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Mono<Void> deleteUserById(@PathVariable Long id) {
        return userService.deleteUserById(id);
    }

    @DeleteMapping("/all")
    @Operation(summary = "Delete all users!!!", description = "Deletes all users!!!")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Mono<Integer> deleteAllUsers() {
        return userService.deleteAllUsers();
    }
}