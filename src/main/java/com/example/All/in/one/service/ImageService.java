package com.example.All.in.one.service;

import com.example.All.in.one.model.ImageItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public ImageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Uploads an image to S3
     */
    public ImageItem uploadImage(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String key = UUID.randomUUID().toString() + "-" + originalFilename;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        ImageItem imageItem = new ImageItem();
        imageItem.setKey(key);
        imageItem.setFilename(originalFilename);
        imageItem.setUrl(getImageUrl(key));
        imageItem.setSize(file.getSize());
        imageItem.setLastModified(Instant.now());
        imageItem.setContentType(file.getContentType());

        return imageItem;
    }

    /**
     * Lists images with pagination
     */
    public Page<ImageItem> listImages(int page, int size) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            List<S3Object> objects = response.contents();

            List<ImageItem> images = objects.stream()
                    .filter(obj -> isImageFile(obj.key()))
                    .map(obj -> {
                        ImageItem item = new ImageItem();
                        item.setKey(obj.key());
                        item.setFilename(getFilenameFromKey(obj.key()));
                        item.setUrl(getImageUrl(obj.key()));
                        item.setSize(obj.size());
                        item.setLastModified(obj.lastModified());
                        return item;
                    })
                    .collect(Collectors.toList());

            // Manual pagination
            Pageable pageable = PageRequest.of(page, size);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), images.size());

            if (start > images.size()) {
                return new PageImpl<>(new ArrayList<>(), pageable, images.size());
            }

            return new PageImpl<>(images.subList(start, end), pageable, images.size());

        } catch (Exception e) {
            log.error("Error listing images", e);
            return Page.empty();
        }
    }

    /**
     * Delete an image from S3
     */
    public void deleteImage(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    /**
     * Constructs a URL for a given S3 object key
     */
    private String getImageUrl(String key) {
        // This is a simple direct S3 URL. In production, you might want to use a CloudFront URL or pre-signed URL
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
    }

    /**
     * Extracts the original filename from the S3 key
     */
    private String getFilenameFromKey(String key) {
        int dashPosition = key.indexOf('-');
        return dashPosition >= 0 && dashPosition < key.length() - 1 ?
                key.substring(dashPosition + 1) : key;
    }

    /**
     * Checks if a file is an image based on its name
     */
    private boolean isImageFile(String filename) {
        String lowerCaseFilename = filename.toLowerCase();
        return lowerCaseFilename.endsWith(".jpg") ||
                lowerCaseFilename.endsWith(".jpeg") ||
                lowerCaseFilename.endsWith(".png") ||
                lowerCaseFilename.endsWith(".gif") ||
                lowerCaseFilename.endsWith(".bmp") ||
                lowerCaseFilename.endsWith(".webp");
    }
}