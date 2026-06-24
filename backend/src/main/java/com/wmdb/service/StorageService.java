package com.wmdb.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class StorageService {

    private MinioClient minioClient;

    @Value("${wmdb.minio.endpoint}")
    private String endpoint;

    @Value("${wmdb.minio.access-key}")
    private String accessKey;

    @Value("${wmdb.minio.secret-key}")
    private String secretKey;

    @Value("${wmdb.minio.bucket}")
    private String bucketName;

    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public StorageResult processSqlFile(MultipartFile file) throws Exception {
        long size = file.getSize();
        StorageResult result = new StorageResult();

        if (size <= 1024 * 1024) { // <= 1MB
            result.setSqlText(new String(file.getBytes(), StandardCharsets.UTF_8));
            result.setAttachmentOssKey(null);
        } else { // > 1MB
            // Upload to MinIO
            String objectKey = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(file.getInputStream(), size, -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Extract summary
            StringBuilder summary = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 50) {
                    summary.append(line).append("\n");
                    count++;
                }
            }
            summary.append("...[内容过长已截断]");

            result.setSqlText(summary.toString());
            result.setAttachmentOssKey(objectKey);
        }

        return result;
    }

    public static class StorageResult {
        private String sqlText;
        private String attachmentOssKey;

        public String getSqlText() {
            return sqlText;
        }

        public void setSqlText(String sqlText) {
            this.sqlText = sqlText;
        }

        public String getAttachmentOssKey() {
            return attachmentOssKey;
        }

        public void setAttachmentOssKey(String attachmentOssKey) {
            this.attachmentOssKey = attachmentOssKey;
        }
    }
}
