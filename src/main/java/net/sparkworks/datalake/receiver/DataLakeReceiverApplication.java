package net.sparkworks.datalake.receiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application for receiving and storing files.
 * Supports both filesystem and MinIO storage backends.
 */
@SpringBootApplication
public class DataLakeReceiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataLakeReceiverApplication.class, args);
    }
}