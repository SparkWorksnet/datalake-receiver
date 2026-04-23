package net.sparkworks.datalake.receiver.storage;

import java.io.IOException;

/**
 * Interface for abstracting storage mechanisms.
 * Implementations can store data to different backends (filesystem, S3, MinIO, etc.)
 */
public interface StorageProvider {

    /**
     * Store data with the specified filename.
     *
     * @param filename the name of the file to store
     * @param data the file content as byte array
     * @throws IOException if storage operation fails
     */
    void store(String filename, byte[] data) throws IOException;

    /**
     * Store data with the specified filename, overriding the default bucket/container.
     * For filesystem providers the bucketName is ignored.
     *
     * @param filename   the name of the file to store
     * @param data       the file content as byte array
     * @param bucketName the target bucket (MinIO) or container; ignored by filesystem providers
     * @throws IOException if storage operation fails
     */
    default void store(String filename, byte[] data, String bucketName) throws IOException {
        store(filename, data);
    }

    /**
     * Initialize the storage provider with necessary setup.
     * For example, creating directories or verifying bucket access.
     *
     * @throws IOException if initialization fails
     */
    void initialize() throws IOException;

    /**
     * Get a descriptive name for this storage provider.
     *
     * @return the name of the storage provider
     */
    String getName();
}