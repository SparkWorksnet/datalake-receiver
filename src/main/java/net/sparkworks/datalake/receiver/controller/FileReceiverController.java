package net.sparkworks.datalake.receiver.controller;

import jakarta.servlet.http.HttpServletRequest;
import net.sparkworks.datalake.receiver.config.AuthProperties;
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
    private static final String FILEPATH_HEADER = "X-file-path";
    private static final String BEARER_PREFIX = "Bearer ";

    private final StorageProvider storageProvider;
    private final AuthProperties authProperties;

    public FileReceiverController(StorageProvider storageProvider, AuthProperties authProperties) {
        this.storageProvider = storageProvider;
        this.authProperties = authProperties;
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

        // Validate access token
        if (!isAuthorized(headers)) {
            logger.warn("Unauthorized access attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized: Invalid or missing access token");
        }
        
        // Extract filename from request path, header, or generate one
        String filepath = extractFilepath(request, headers);
        String filename = extractFilename(request, headers);
        
        final String fullFilename = filepath == null ? filename : filepath + "/" + filename;
        
        try {
            logger.info("Storing file with filename: {} ({} bytes)", fullFilename, data.length);
            storageProvider.store(fullFilename, data);
            return ResponseEntity.ok("File stored successfully: " + fullFilename);
        } catch (IOException e) {
            logger.error("Failed to store file: " + fullFilename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to store file: " + e.getMessage());
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
     * Extract filepath from request header.
     * Priority: request path > X-file-path header > timestamp
     *
     * @param request HTTP servlet request
     * @param headers HTTP headers
     * @return filename to use for storing the file
     */
    private String extractFilepath(HttpServletRequest request, HttpHeaders headers) {
        // Try to get filename from request path
        String requestPath = request.getRequestURI();
        logger.debug("Request URI: {}", requestPath);
        
        String headerFilepath = headers.getFirst(FILEPATH_HEADER);
        if (headerFilepath != null && !headerFilepath.isBlank()) {
            logger.debug("Using filepath from header: {}", headerFilepath);
            return headerFilepath;
        }
        return null;
    }

    /**
     * Validate the Authorization header contains the correct access token.
     * Supports Bearer token format: "Bearer {token}"
     *
     * @param headers HTTP headers
     * @return true if authorized, false otherwise
     */
    private boolean isAuthorized(HttpHeaders headers) {
        // Skip validation if authentication is disabled
        if (!authProperties.isEnabled()) {
            logger.debug("Authentication is disabled");
            return true;
        }

        // Check if access token is configured
        if (authProperties.getAccessToken() == null || authProperties.getAccessToken().isBlank()) {
            logger.warn("Access token is not configured but authentication is enabled");
            return false;
        }

        // Get Authorization header
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank()) {
            logger.debug("Missing Authorization header");
            return false;
        }

        // Validate Bearer token format
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            logger.debug("Authorization header does not start with 'Bearer '");
            return false;
        }

        // Extract and validate token
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        boolean isValid = authProperties.getAccessToken().equals(token);

        if (!isValid) {
            logger.debug("Invalid access token provided");
        }

        return isValid;
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
