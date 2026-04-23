package net.sparkworks.datalake.receiver.service;

import net.sparkworks.datalake.receiver.config.DaliConnectorProperties;
import net.sparkworks.datalake.receiver.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers received files as assets in the DALI EDC connector Management API.
 */
@Service
public class EdcAssetRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(EdcAssetRegistrationService.class);

    private final DaliConnectorProperties properties;
    private final StorageProperties storageProperties;
    private final RestClient restClient;

    public EdcAssetRegistrationService(DaliConnectorProperties properties, StorageProperties storageProperties) {
        this.properties = properties;
        this.storageProperties = storageProperties;
        this.restClient = RestClient.create();
        logger.info("connectorUrl: {}", properties.getUrl());
    }

    /**
     * Register a stored file as an asset in the DALI EDC connector.
     *
     * @param assetId    the asset ID (typically the stored object key / filename)
     * @param filePath   the full path/key of the stored file (used as prefix in the dataAddress)
     * @param bucketName the MinIO bucket name extracted from the X-file-path header
     */
    public void registerAsset(String assetId, String filePath, String bucketName) {
        if (!StringUtils.hasText(properties.getUrl())) {
            logger.debug("DALI connector URL not configured, skipping asset registration");
            return;
        }

        Map<String, String> dataAddress = new HashMap<>();
        dataAddress.put("type", "MinioFiles");
        dataAddress.put("bucketName", bucketName);
        dataAddress.put("prefix", filePath);
        dataAddress.put("endpoint", storageProperties.getMinio().getEndpoint());
        dataAddress.put("accessKey", storageProperties.getMinio().getAccessKey());
        dataAddress.put("secretKey", storageProperties.getMinio().getSecretKey());

        Map<String, String> context = new HashMap<>();
        context.put("@vocab", "https://w3id.org/edc/v0.0.1/ns/");

        Map<String, Object> asset = new HashMap<>();
        asset.put("@context", context);
        asset.put("@id", assetId);
        asset.put("properties", new HashMap<>());
        asset.put("dataAddress", dataAddress);

        String url = properties.getUrl() + "/management/v3/assets";

        logger.info("Registering asset '{}' to DALI connector: {}", assetId, url);

        try {
            restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(asset)
                .retrieve()
                .toBodilessEntity();

            logger.info("✓ Asset '{}' registered to DALI connector", assetId);
        } catch (Exception e) {
            logger.warn("⚠ Failed to register asset '{}' to DALI connector: {}", assetId, e.getMessage());
        }
    }
}
