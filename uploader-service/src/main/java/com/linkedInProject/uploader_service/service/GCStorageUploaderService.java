package com.linkedInProject.uploader_service.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GCStorageUploaderService implements UploaderService{

    private final Storage storage;

    @Value("${spring.cloud.gcp.storage-bucket-name}")
    private String bucketName ;

    @Override
    public String upload(MultipartFile file) {
        try{
            String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName,filename).build();
            storage.create(blobInfo,file.getBytes());
            return String.format("https://storage.googleapis.com/%s/%s",bucketName,filename);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
