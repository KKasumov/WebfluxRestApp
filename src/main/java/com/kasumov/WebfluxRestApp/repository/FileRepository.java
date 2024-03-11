package com.kasumov.WebfluxRestApp.repository;

import com.kasumov.WebfluxRestApp.model.File;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileRepository extends R2dbcRepository<File, Long> {

    Mono<File> findActiveById(Long id);

    Flux<File> findAllActive();

    Flux<File> findAllActiveByUserId(Long userId);

    Mono<Long> getIdByFileName(String fileName);

    Mono<Void> deleteActiveById(Long id);

    Mono<Integer> deleteAllActiveByUserId(Long userId);

    Mono<Integer> deleteAllActive();
}