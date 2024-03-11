package com.kasumov.WebfluxRestApp.service.impl;

import com.kasumov.WebfluxRestApp.dto.EventDTO;
import com.kasumov.WebfluxRestApp.mapper.EventMapper;
import com.kasumov.WebfluxRestApp.model.Event;
import com.kasumov.WebfluxRestApp.repository.EventRepository;
import com.kasumov.WebfluxRestApp.repository.FileRepository;
import com.kasumov.WebfluxRestApp.security.CustomPrincipal;
import com.kasumov.WebfluxRestApp.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.kasumov.WebfluxRestApp.security.SecurityUtils.isAdminOrModerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final FileRepository fileRepository;
    private final EventMapper eventMapper;

    @Override
    public Mono<EventDTO> getEventByIdAndAuth(Long id, Mono<Authentication> authMono) {
        log.info("GetEventByIdAndAuth: {}", id);
        return authMono
                .flatMap(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return isAdminOrModerator(Mono.just(authentication))
                            .flatMap(isAdminOrModerator -> eventRepository.findActiveById(id)
                                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, String.valueOf(id))))
                                    .flatMap(event -> {
                                        if (isAdminOrModerator || event.getUserId().equals(principal.getId())) {
                                            return fileRepository.findActiveById(event.getFileId())
                                                    .map(file -> eventMapper.map(event, file));
                                        } else {
                                            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                                        }
                                    }));
                })
                .doOnError(error -> log.error(error.getMessage(), id));
    }

    @Override
    public Flux<EventDTO> getAllEventsByAuth(Mono<Authentication> authMono) {
        log.info("GetAllEventsByAuth");
        return authMono
                .flatMapMany(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return isAdminOrModerator(Mono.just(authentication))
                            .flatMapMany(isAdminOrModerator -> {
                                if (isAdminOrModerator) {
                                    return eventRepository.findAllActive();
                                } else {
                                    return eventRepository.findAllActiveByUserId(principal.getId());
                                }
                            })
                            .flatMap(event -> fileRepository.findActiveById(event.getFileId())
                                    .map(file -> eventMapper.map(event, file)));
                })
                .doOnError(error -> log.error(error.getMessage()));
    }

    @Override
    public Flux<EventDTO> getEventsByUserId(Long userId) {
        log.info("GetEventsByUserId: {}", userId);
        return eventRepository.findAllActiveByUserId(userId)
                .flatMap(event -> fileRepository.findActiveById(event.getFileId())
                        .map(file -> eventMapper.map(event, file)))
                .doOnError(error -> log.error(error.getMessage()));
    }

    @Override
    public Mono<Event> getEventByFileNameAndUserId(String fileName, Long userId) {
        log.info("GetEventByFileNameAndUserId: {}, {}", fileName, userId);
        return fileRepository.getIdByFileName(fileName)
                .flatMap(fileId -> eventRepository.findActiveByFileIdAndUserId(fileId, userId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")));
    }

    @Override
    public Mono<EventDTO> updateEventById(Long id, EventDTO eventDTO) {
        log.info("UpdateEventById: {}", eventDTO);
        return eventRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, String.valueOf(id))))
                .flatMap(foundEvent -> {
                    foundEvent.setUserId(eventDTO.getUserId());
                    foundEvent.setFileId(eventDTO.getFileId());
                    return eventRepository.save(foundEvent);
                })
                .map(eventMapper::map)
                .doOnError(error -> log.error(error.getMessage(), id));
    }

    @Override
    public Mono<Void> deleteEventById(Long id) {
        log.info("DeleteEventById: {}", id);
        return eventRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, String.valueOf(id))))
                .flatMap(file -> {
                    log.info(String.valueOf(id));
                    return eventRepository.deleteActiveById(id);
                })
                .then()
                .doOnError(error -> log.error(error.getMessage(), id));
    }

    @Override
    public Mono<Integer> deleteAllEvents() {
        log.info("DeleteAllEvents");
        return eventRepository.deleteAllActive()
                .doOnError(error -> log.error(error.getMessage()));
    }
}