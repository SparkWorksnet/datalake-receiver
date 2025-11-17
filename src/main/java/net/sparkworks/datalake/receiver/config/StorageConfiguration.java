package net.sparkworks.datalake.receiver.config;

import net.sparkworks.datalake.receiver.storage.FileSystemStorageProvider;
import net.sparkworks.datalake.receiver.storage.MinIOStorageProvider;
import net.sparkworks.datalake.receiver.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Spring configuration for storage providers.
 */
@Configuration
public class StorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(StorageConfiguration.class);

    @Bean
    public StorageProvider storageProvider(StorageProperties properties) throws IOException {
        StorageProvider provider;

        logger.info("Creating storage provider of type: {}", properties.getType());

        switch (properties.getType()) {
            case FILESYSTEM -> provider = createFileSystemProvider(properties.getFilesystem());
            case MINIO -> provider = createMinIOProvider(properties.getMinio());
            default -> {
                logger.warn("Unknown storage type '{}', falling back to filesystem", properties.getType());
                provider = createFileSystemProvider(properties.getFilesystem());
            }
        }

        // Initialize the storage provider
        provider.initialize();
        logger.info("Storage provider initialized: {}", provider.getName());

        return provider;
    }

    private StorageProvider createFileSystemProvider(StorageProperties.FileSystemConfig config) {
        logger.info("Creating FileSystem storage provider with directory: {}", config.getDirectory());
        return new FileSystemStorageProvider(config.getDirectory());
    }

    private StorageProvider createMinIOProvider(StorageProperties.MinioConfig config) {
        if (config.getEndpoint() == null || config.getEndpoint().isBlank()) {
            throw new IllegalStateException("MinIO endpoint is required when storage.type=minio");
        }
        if (config.getAccessKey() == null || config.getAccessKey().isBlank()) {
            throw new IllegalStateException("MinIO access key is required when storage.type=minio");
        }
        if (config.getSecretKey() == null || config.getSecretKey().isBlank()) {
            throw new IllegalStateException("MinIO secret key is required when storage.type=minio");
        }

        logger.info("Creating MinIO storage provider - Endpoint: {}, Bucket: {}",
                config.getEndpoint(), config.getBucketName());
        return new MinIOStorageProvider(
                config.getEndpoint(),
                config.getAccessKey(),
                config.getSecretKey(),
                config.getBucketName()
        );
    }
}