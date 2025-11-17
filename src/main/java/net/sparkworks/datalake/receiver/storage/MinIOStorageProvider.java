package net.sparkworks.datalake.receiver.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Storage provider that stores files to a MinIO bucket.
 */
public class MinIOStorageProvider implements StorageProvider {

    private static final Logger logger = LoggerFactory.getLogger(MinIOStorageProvider.class);

    private final MinioClient minioClient;
    private final String bucketName;

    /**
     * Creates a MinIO storage provider.
     *
     * @param endpoint the MinIO server endpoint (e.g., "http://localhost:9000")
     * @param accessKey the access key for authentication
     * @param secretKey the secret key for authentication
     * @param bucketName the bucket name where files will be stored
     */
    public MinIOStorageProvider(String endpoint, String accessKey, String secretKey, String bucketName) {
        this.bucketName = bucketName;
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Override
    public void initialize() throws IOException {
        try {
            // Check if bucket exists
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!bucketExists) {
                logger.info("Creating MinIO bucket: {}", bucketName);
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                logger.info("Successfully created bucket: {}", bucketName);
            } else {
                logger.info("MinIO bucket already exists: {}", bucketName);
            }

            logger.info("MinIO storage initialized - Bucket: {}", bucketName);
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new IOException("Failed to initialize MinIO storage", e);
        }
    }

    @Override
    public void store(String filename, byte[] data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

            logger.info("Storing file to MinIO bucket '{}': {}", bucketName, filename);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(inputStream, data.length, -1)
                            .contentType("application/octet-stream")
                            .build()
            );

            logger.info("Successfully stored file to MinIO: {} ({} bytes)", filename, data.length);
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new IOException("Failed to store file to MinIO: " + filename, e);
        }
    }

    @Override
    public String getName() {
        return "MinIO [bucket: " + bucketName + "]";
    }
}