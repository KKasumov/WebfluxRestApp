package com.kasumov.WebfluxRestApp.service;

import com.kasumov.WebfluxRestApp.dto.FileDTO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface FileStorageService {

    Mono<FileDTO> uploadUserFileToStorage(FilePart filePart, Mono<Authentication> authMono);

    Mono<ResponseEntity<Resource>> downloadFileFromStorageByFileNameAndAuth(String fileName, Mono<Authentication> authMono);
}
