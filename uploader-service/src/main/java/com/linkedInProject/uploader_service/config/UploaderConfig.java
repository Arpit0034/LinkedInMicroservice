package com.linkedInProject.uploader_service.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.StoredFile;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
public class UploaderConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey ;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary(){
        Map<String,String> config = Map.of(
                "cloud_name",cloudName,
                "api_key",apiKey,
                "api_secret",apiSecret
        );
        return new Cloudinary(config);
    }

    @Value("${spring.cloud.gcp.credentials.location}")
    private String credentialsLocation ;

    private final ResourceLoader resourceLoader ;

    @Bean
    public Storage storage() throws IOException {
        Resource resource = resourceLoader.getResource(credentialsLocation);

        if (!resource.exists()) {
            throw new IllegalArgumentException("GCP credentials file not found at: " + credentialsLocation);
        }

        return StorageOptions.newBuilder()
                .setCredentials(ServiceAccountCredentials.fromStream(
                        resource.getInputStream()
                ))
                .build()
                .getService();
    }
}
