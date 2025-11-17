package net.sparkworks.datalake.receiver.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Storage provider that stores files to the local filesystem.
 */
public class FileSystemStorageProvider implements StorageProvider {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemStorageProvider.class);

    private final String directory;

    /**
     * Creates a filesystem storage provider.
     *
     * @param directory the directory where files will be stored
     */
    public FileSystemStorageProvider(String directory) {
        this.directory = directory;
    }

    @Override
    public void initialize() throws IOException {
        Path filesPath = Paths.get(directory);
        if (!Files.exists(filesPath)) {
            logger.info("Creating directory: {}", directory);
            Files.createDirectories(filesPath);
        }
        logger.info("FileSystem storage initialized at: {}", directory);
    }

    @Override
    public void store(String filename, byte[] data) throws IOException {
        Path path = Paths.get("%s/%s".formatted(directory, filename));
        logger.info("Storing file to filesystem: {}", path);
        Files.write(path, data);
        logger.info("Successfully stored file: {} ({} bytes)", filename, data.length);
    }

    @Override
    public String getName() {
        return "FileSystem [" + directory + "]";
    }
}