package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StorageService {

    private final S3Client s3Client;
    private final Path localStoragePath = Paths.get("/tmp/cloudapp-media");

    @Value("${app.aws-enabled:false}")
    private boolean awsEnabled;

    @Value("${app.s3-bucket}")
    private String bucketName;

    public StorageService(S3Client s3Client) throws IOException {
        this.s3Client = s3Client;
        Files.createDirectories(localStoragePath);
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String cleanName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";
        String filename = System.currentTimeMillis() + "_" + cleanName;

        if (awsEnabled) {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putOb, RequestBody.fromBytes(file.getBytes()));
        } else {
            Files.write(localStoragePath.resolve(filename), file.getBytes());
        }
        return filename;
    }

    public byte[] downloadFile(String filename) throws IOException {
        if (awsEnabled) {
            GetObjectRequest getOb = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();
            return s3Client.getObjectAsBytes(getOb).asByteArray();
        } else {
            return Files.readAllBytes(localStoragePath.resolve(filename));
        }
    }
}