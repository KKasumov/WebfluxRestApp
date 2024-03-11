package com.kasumov.WebfluxRestApp.repository;

import com.kasumov.WebfluxRestApp.dto.FileDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface FileStorageRepository {

    Mono<FileDTO> uploadUserFileToStorage(FilePart filePart);
    Mono<ResponseEntity<ByteArrayResource>> downloadFileFromStorage(String fileName);
}
