package com.kasumov.WebfluxRestApp.repository;

import com.kasumov.WebfluxRestApp.model.UserEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<UserEntity, Long> {

    Mono<Boolean> existsByUsernameAndIdNot(String username, Long id);

    Mono<UserEntity> findActiveByUsername(String username);

    Mono<UserEntity> findActiveById(Long id);

    Flux<UserEntity> findAllActive();

    @Modifying
    Mono<Void> deleteActiveById(Long id);

    @Modifying
    Mono<Integer> deleteAllActive();
}