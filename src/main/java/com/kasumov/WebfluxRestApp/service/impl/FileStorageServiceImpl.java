package com.kasumov.WebfluxRestApp.service.impl;

import com.kasumov.WebfluxRestApp.dto.FileDTO;
import com.kasumov.WebfluxRestApp.model.Event;
import com.kasumov.WebfluxRestApp.model.File;
import com.kasumov.WebfluxRestApp.model.UserRole;
import com.kasumov.WebfluxRestApp.repository.EventRepository;
import com.kasumov.WebfluxRestApp.repository.FileRepository;
import com.kasumov.WebfluxRestApp.repository.FileStorageRepository;
import com.kasumov.WebfluxRestApp.security.CustomPrincipal;
import com.kasumov.WebfluxRestApp.service.EventService;
import com.kasumov.WebfluxRestApp.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.Collection;
import java.util.Collections;


@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.s3.bucket-name}")
    private String s3BucketName;

    private String S3_FILE_LOCATION;

    @PostConstruct
    private void init() {
        S3_FILE_LOCATION = String.format("https://%s.s3.amazonaws.com/", s3BucketName);
    }

    private final FileStorageRepository fileStorageRepository;
    private final EventService eventService;
    private final FileRepository fileRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public Mono<FileDTO> uploadUserFileToStorage(FilePart filePart, Mono<Authentication> authMono) {
        log.info("UploadUserFileToStorage:");
        return authMono
                .flatMap(auth ->
                        extractUserId(auth)
                                .map(userId -> new UserContext(userId, auth.getAuthorities())))
                .flatMap(userContext -> {
                    long userId = userContext.userId();
                    String filename = filePart.filename();
                    String location = S3_FILE_LOCATION + filename;

                    File file = File.builder()
                            .location(location)
                            .build();

                    return fileRepository.save(file)
                            .flatMap(savedFile -> {
                                Event event = Event.builder()
                                        .userId(userId)
                                        .fileId(savedFile.getId())
                                        .build();

                                return eventRepository.save(event);
                            })
                            .then(fileStorageRepository.uploadUserFileToStorage(filePart))
                            .doOnSuccess(unused -> log.info("UPLOADED_SUCCESSFULLY_WITH_FILENAME_AND_USER_ID", filename, userId))
                            .doOnError(error -> log.error(filename, userId, error.getMessage()));
                });
    }

    private Mono<Long> extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomPrincipal customPrincipal) {
            return Mono.just(customPrincipal.getId());
        }
        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
    }

    private record UserContext(
            Long userId,
            Collection<? extends GrantedAuthority> authorities) {
    }

    @Override
    public Mono<ResponseEntity<Resource>> downloadFileFromStorageByFileNameAndAuth(String fileName, Mono<Authentication> authMono) {
        log.info("DownloadFileFromStorageByFileName: {}", fileName);
        return authMono
                .flatMap(auth -> extractUserId(auth).map(userId -> new UserContext(userId, auth.getAuthorities())))
                .defaultIfEmpty(new UserContext(null, Collections.emptyList()))
                .flatMap(userContext -> checkUserAccessToFile(fileName, userContext.userId, userContext.authorities))
                .flatMap(hasAccess -> {
                    if (!hasAccess) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                    return downloadFile(fileName);
                })
                .onErrorMap(this::handleDownloadError)
                .doOnSuccess(unused -> log.info("DOWNLOADED_SUCCESSFULLY_WITH_FILENAME", fileName))
                .doOnError(error -> log.error(fileName, error.getMessage()));
    }

    private Mono<Boolean> checkUserAccessToFile(String fileName, Long userId, Collection<? extends GrantedAuthority> authorities) {
        if (authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + UserRole.USER.name()))) {
            return eventService.getEventByFileNameAndUserId(fileName, userId)
                    .map(e -> true)
                    .defaultIfEmpty(false);
        }
        return Mono.just(true);
    }

    private Mono<ResponseEntity<Resource>> downloadFile(String fileName) {
        return fileStorageRepository.downloadFileFromStorage(fileName)
                .map(responseEntity -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(responseEntity.getBody()));
    }

    private Throwable handleDownloadError(Throwable error) {
        if (error instanceof NoSuchKeyException) {
            log.error(error.getMessage(), error);
            return new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return error;
    }
}
