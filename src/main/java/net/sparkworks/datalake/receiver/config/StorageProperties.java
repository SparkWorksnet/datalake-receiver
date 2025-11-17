package net.sparkworks.datalake.receiver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for storage backends.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * Storage type: filesystem or minio
     */
    private StorageType type = StorageType.FILESYSTEM;

    /**
     * Filesystem storage configuration
     */
    private FileSystemConfig filesystem = new FileSystemConfig();

    /**
     * MinIO storage configuration
     */
    private MinioConfig minio = new MinioConfig();

    public enum StorageType {
        FILESYSTEM,
        MINIO
    }

    @Data
    public static class FileSystemConfig {
        /**
         * Directory path where files will be stored
         */
        private String directory = "files";
    }

    @Data
    public static class MinioConfig {
        /**
         * MinIO server endpoint
         */
        private String endpoint;

        /**
         * MinIO access key
         */
        private String accessKey;

        /**
         * MinIO secret key
         */
        private String secretKey;

        /**
         * MinIO bucket name
         */
        private String bucketName = "data-lake";
    }
}