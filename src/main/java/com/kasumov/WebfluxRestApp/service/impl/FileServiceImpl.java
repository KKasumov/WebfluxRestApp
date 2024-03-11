package com.kasumov.WebfluxRestApp.service.impl;

import com.kasumov.WebfluxRestApp.model.File;
import com.kasumov.WebfluxRestApp.repository.EventRepository;
import com.kasumov.WebfluxRestApp.repository.FileRepository;
import com.kasumov.WebfluxRestApp.security.CustomPrincipal;
import com.kasumov.WebfluxRestApp.service.FileService;
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
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final EventRepository eventRepository;

    @Override
    public Mono<File> getFileByIdAndAuth(Long id, Mono<Authentication> authMono) {
        log.info("GetFileByIdAndAuth: {}", id);
        return authMono
                .flatMap(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return isAdminOrModerator(Mono.just(authentication))
                            .flatMap(isAdminOrModerator -> {
                                if (isAdminOrModerator) {
                                    return fileRepository.findActiveById(id);
                                } else {
                                    return existsByIdAndUserId(id, principal.getId())
                                            .flatMap(exists -> exists ?
                                                    fileRepository.findActiveById(id) :
                                                    Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));
                                }
                            });
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(String.valueOf(id)))))
                .doOnSuccess(unused -> log.info("SUCCESSFULLY_WITH_ID", id))
                .doOnError(error -> log.error(error.getMessage(), id));
    }

    @Override
    public Mono<Boolean> existsByIdAndUserId(Long fileId, Long userId) {
        log.info(String.valueOf(fileId), userId);
        return eventRepository.findActiveByFileIdAndUserId(fileId, userId)
                .map(e -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Flux<File> getAllFilesByAuth(Mono<Authentication> authMono) {
        log.info("GetAllFilesByAuth:");
        return authMono.flatMapMany(authentication -> {
                    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
                    return isAdminOrModerator(Mono.just(authentication))
                            .flatMapMany(isAdminOrModerator -> {
                                if (isAdminOrModerator) {
                                    return fileRepository.findAllActive();
                                } else {
                                    return fileRepository.findAllActiveByUserId(principal.getId());
                                }
                            });
                })
                .doOnComplete(() -> log.info("FINISHED_SUCCESSFULLY"))
                .doOnError(error -> log.error(error.getMessage()));
    }

    @Override
    public Flux<File> getFilesByUserId(Long userId) {
        log.info("GetFilesByUserId: {}", userId);
        return fileRepository.findAllActiveByUserId(userId)
                .doOnComplete(() -> log.info("FINISHED_SUCCESSFULLY"))
                .doOnError(error -> log.error(error.getMessage()));
    }

    @Override
    public Mono<File> updateFileById(Long id, File file) {
        log.info("UpdateFileById: {}", file);
        return fileRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(String.valueOf(id)))))
                .flatMap(foundFile -> {
                    foundFile.setLocation(file.getLocation());
                    return fileRepository.save(foundFile);
                })
                .doOnSuccess(aVoid -> log.info("SUCCESSFULLY_WITH_ID", id))
                .doOnError(error -> log.error(error.getMessage(), id));
    }

    @Override
    public Mono<Void> deleteFileById(Long id) {
        log.info("DeleteFileById: '{}'", id);
        return fileRepository.findActiveById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(String.valueOf(id)))))
                .flatMap(file -> {
                    log.info("DELETING_FILE_WITH_ID", id);
                    return fileRepository.deleteActiveById(id);
                })
                .then()
                .doOnSuccess(aVoid -> log.info("DELETED_SUCCESSFULLY_WITH_ID", id))
                .doOnError(error -> log.error(error.getMessage(), id));
    }

    @Override
    public Mono<Integer> deleteAllFilesByUserId(Long userId) {
        log.info("DeleteAllFilesByUserId: {}", userId);
        return fileRepository.deleteAllActiveByUserId(userId)
                .doOnTerminate(() -> log.info("DELETED_SUCCESSFULLY_WITH_USER_ID", userId))
                .doOnError(error -> log.error(error.getMessage(), userId));
    }

    @Override
    public Mono<Integer> deleteAllFiles() {
        log.info("DeleteAllFiles");
        return fileRepository.deleteAllActive()
                .doOnTerminate(() -> log.info("DELETED_SUCCESSFULLY"))
                .doOnError(error -> log.error(error.getMessage()));
    }
}
