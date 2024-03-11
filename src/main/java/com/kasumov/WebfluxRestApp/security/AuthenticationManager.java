package com.kasumov.WebfluxRestApp.security;

import com.kasumov.WebfluxRestApp.exception.SecurityException;
import com.kasumov.WebfluxRestApp.model.UserEntity;
import com.kasumov.WebfluxRestApp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {

    private final UserService userService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        return userService.getUserById(principal.getId())
                .filter(UserEntity::isEnabled)
                .switchIfEmpty(Mono.error(new SecurityException.UnauthorizedException("User disabled")))
                .map(user -> authentication);
    }
}
