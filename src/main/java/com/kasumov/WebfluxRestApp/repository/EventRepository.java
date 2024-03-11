package com.kasumov.WebfluxRestApp.repository;

import com.kasumov.WebfluxRestApp.model.Event;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventRepository extends R2dbcRepository<Event, Long> {

    Mono<Event> findActiveById(Long id);

    Flux<Event> findAllActive();

    Flux<Event> findAllActiveByUserId(Long userId);

    Mono<Event> findActiveByFileIdAndUserId(Long fileId, Long userId);

    Mono<Void> deleteActiveById(Long id);

    Mono<Integer> deleteAllActive();
}