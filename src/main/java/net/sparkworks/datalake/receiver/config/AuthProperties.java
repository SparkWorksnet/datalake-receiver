package net.sparkworks.datalake.receiver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for authentication.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /**
     * Whether authentication is enabled.
     * Set to false to disable authentication (useful for testing).
     */
    private boolean enabled = true;

    /**
     * The access token required for API authentication.
     * Clients must provide this token in the Authorization header as "Bearer {token}".
     */
    private String accessToken;
}