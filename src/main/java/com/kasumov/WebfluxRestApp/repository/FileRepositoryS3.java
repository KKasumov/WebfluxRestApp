package com.kasumov.WebfluxRestApp.repository;

import com.kasumov.WebfluxRestApp.dto.FileDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Slf4j
@Component
public class FileRepositoryS3 implements FileStorageRepository {

    private static final Region AWS_S3_REGION_EU_CENTRAL_1 = Region.EU_CENTRAL_1;
    private static final String TMP_DIR_PATH = "/tmp/myapp";
    private static final String TEMP_FILE_NAME_PREFIX = "tmp-file-";

    @Value("${app.s3.bucket-name}")
    private String bucketName;

    @Value("${app.s3.key-prefix}")
    private String keyPrefix;

    @Value("${app.s3.aws-access-key-id}")
    private String awsAccessKeyId;

    @Value("${app.s3.aws-secret-access-key}")
    private String awsSecretAccessKey;

    private final S3AsyncClient s3Client;

    public FileRepositoryS3() {
        this.s3Client = S3AsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(this.awsAccessKeyId, this.awsSecretAccessKey)))
                .region(AWS_S3_REGION_EU_CENTRAL_1)
                .build();
    }

    @Override
    public Mono<FileDTO> uploadUserFileToStorage(FilePart filePart) {
        String fileName = filePart.filename();
        Path tempFile = Paths.get(TMP_DIR_PATH, TEMP_FILE_NAME_PREFIX + fileName);

        return Mono.from(filePart.transferTo(tempFile.toFile()))
                .then(Mono.fromCompletionStage(() ->
                        s3Client.putObject(PutObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(keyPrefix + "/" + fileName)
                                        .build(),
                                (Path) filePart.content()))
                )
                .doOnSuccess(response -> {
                    log.info("UPLOADED_SUCCESSFULLY_TO_S3: {}", fileName);
                    tempFile.toFile().delete();
                })
                .doOnError(error -> log.error("ERROR_UPLOADING_TO_S3: {}", error.getMessage()))
                .thenReturn(new FileDTO(fileName, LocalDateTime.now()));
    }

    @Override
    public Mono<ResponseEntity<ByteArrayResource>> downloadFileFromStorage(String fileName) {
        String key = keyPrefix + "/" + fileName;

        return Mono.fromCompletionStage(() ->
                        s3Client.getObject(GetObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(key)
                                        .build(),
                                AsyncResponseTransformer.toBytes()))
                .map(responseBytes ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .body(new ByteArrayResource(responseBytes.asByteArray()))
                )
                .doOnSuccess(response -> log.info("DOWNLOADED_SUCCESSFULLY_FROM_S3: {}", fileName))
                .doOnError(error -> log.error("ERROR_DOWNLOADING_FROM_S3: {}", fileName, error));
    }
}