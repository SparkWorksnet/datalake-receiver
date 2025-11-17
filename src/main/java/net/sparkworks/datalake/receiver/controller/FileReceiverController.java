package net.sparkworks.datalake.receiver.controller;

import jakarta.servlet.http.HttpServletRequest;
import net.sparkworks.datalake.receiver.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * REST controller for receiving and storing files.
 */
@RestController
@RequestMapping("/")
public class FileReceiverController {

    private static final Logger logger = LoggerFactory.getLogger(FileReceiverController.class);
    private static final String FILENAME_HEADER = "X-file-name";

    private final StorageProvider storageProvider;

    public FileReceiverController(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    /**
     * Receive and store a file.
     * <p>
     * Filename resolution priority:
     * 1. Request path (e.g., POST /a.txt or POST /b/c.json)
     * 2. X-file-name header
     * 3. Auto-generated timestamp-based name
     *
     * @param request HTTP servlet request
     * @param headers HTTP headers
     * @param data file content as byte array
     * @return HTTP 200 on success, 500 on error
     */
    @PostMapping("/**")
    public ResponseEntity<String> receiveFile(
            HttpServletRequest request,
            @RequestHeader HttpHeaders headers,
            @RequestBody byte[] data) {

        // Log all headers
        headers.forEach((key, value) -> logger.info("header[{}]={}", key, value));

        // Extract filename from request path, header, or generate one
        String filename = extractFilename(request, headers);

        try {
            logger.info("Storing file with filename: {} ({} bytes)", filename, data.length);
            storageProvider.store(filename, data);
            return ResponseEntity.ok("File stored successfully: " + filename);
        } catch (IOException e) {
            logger.error("Failed to store file: " + filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to store file: " + e.getMessage());
        }
    }

    /**
     * Extract filename from request path, header, or generate timestamp-based name.
     * Priority: request path > X-file-name header > timestamp
     *
     * @param request HTTP servlet request
     * @param headers HTTP headers
     * @return filename to use for storing the file
     */
    private String extractFilename(HttpServletRequest request, HttpHeaders headers) {
        // Try to get filename from request path
        String requestPath = request.getRequestURI();
        logger.debug("Request URI: {}", requestPath);

        // Remove leading slash and use path as filename if not empty
        if (requestPath != null && !requestPath.equals("/")) {
            String filename = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
            if (!filename.isBlank()) {
                logger.debug("Using filename from request path: {}", filename);
                return filename;
            }
        }

        // Fall back to X-file-name header
        String headerFilename = headers.getFirst(FILENAME_HEADER);
        if (headerFilename != null && !headerFilename.isBlank()) {
            logger.debug("Using filename from header: {}", headerFilename);
            return headerFilename;
        }

        // Fall back to timestamp-based name
        String generatedFilename = "%d.data".formatted(System.currentTimeMillis());
        logger.debug("Using generated filename: {}", generatedFilename);
        return generatedFilename;
    }

    /**
     * Health check endpoint.
     *
     * @return HTTP 200 with storage provider information
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK - Storage: " + storageProvider.getName());
    }
}