package com.kasumov.WebfluxRestApp.service;


import com.kasumov.WebfluxRestApp.dto.EventDTO;
import com.kasumov.WebfluxRestApp.model.Event;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventService {

    Mono<EventDTO> getEventByIdAndAuth(Long id, Mono<Authentication> authMono);

    Flux<EventDTO> getAllEventsByAuth(Mono<Authentication> authMono);

    Flux<EventDTO> getEventsByUserId(Long userId);

    Mono<Event> getEventByFileNameAndUserId(String fileName, Long userId);

    Mono<EventDTO> updateEventById(Long id, EventDTO eventDTO);

    Mono<Void> deleteEventById(Long id);

    Mono<Integer> deleteAllEvents();
}
