# Data Lake Receiver - Spring Boot Application

A Spring Boot REST API service for receiving and storing files to various storage backends (filesystem or MinIO).

## Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
  - [Filesystem Storage](#run-with-default-configuration-filesystem-storage)
  - [MinIO Storage](#run-with-minio-storage)
  - [Environment Variables](#using-environment-variables)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
  - [Configuration Sources](#configuration-sources)
  - [Configuration Properties](#configuration-properties)
  - [Spring Profiles](#spring-profiles)
  - [Quick Examples](#configuration-quick-examples)
- [Storage Backends](#storage-backends)
  - [Filesystem Storage](#filesystem-storage-details)
  - [MinIO Storage](#minio-storage-details)
- [Startup Banner](#startup-banner)
- [Building from Source](#building-from-source)
- [Docker Deployment](#docker-deployment)
- [Development](#development)
- [Logging](#logging)
- [Monitoring and Health](#monitoring-and-health)
- [Troubleshooting](#troubleshooting)
- [Migration Guide](#migration-from-non-spring-version)

---

## Features

- **RESTful API** - Simple HTTP POST endpoint for file uploads
- **Multiple Storage Backends** - Filesystem or MinIO object storage
- **Spring Boot** - Production-ready with built-in health checks, metrics, and configuration management
- **Flexible Configuration** - YAML-based configuration with profile support and environment variable overrides
- **Automatic Initialization** - Storage backends are initialized automatically on startup
- **Custom Banner** - Branded ASCII art banner showing application status
- **Comprehensive Logging** - Configurable logging with multiple levels

---

## Quick Start

### Run with Default Configuration (Filesystem Storage)

```bash
java -jar data-lake-receiver-1.0-SNAPSHOT.jar
```

This will start the server on port 4000 and store files in the `./files` directory.

**You'll see a custom ASCII banner on startup:**
```
  ____        _          _           _          ____               _
 |  _ \  __ _| |_ __ _  | |    __ _ | | _____  |  _ \ ___  ___ ___(_)_   _____ _ __
 | | | |/ _` | __/ _` | | |   / _` || |/ / _ \ | |_) / _ \/ __/ _ \ \ \ / / _ \ '__|
 | |_| | (_| | || (_| | | |__| (_| ||   <  __/ |  _ <  __/ (_|  __/ |\ V /  __/ |
 |____/ \__,_|\__\__,_| |_____\__,_||_|\_\___| |_| \_\___|\___\___|_| \_/ \___|_|

 :: Spring Boot 3.2.0 ::
 :: Application: Data Lake Receiver ::
 :: Storage: FILESYSTEM ::
```

### Run with MinIO Storage

```bash
java -jar data-lake-receiver-1.0-SNAPSHOT.jar --spring.profiles.active=minio \
  --storage.minio.endpoint=http://localhost:9000 \
  --storage.minio.access-key=minioadmin \
  --storage.minio.secret-key=minioadmin
```

### Using Environment Variables

```bash
export SERVER_PORT=8080
export STORAGE_TYPE=MINIO
export STORAGE_MINIO_ENDPOINT=http://localhost:9000
export STORAGE_MINIO_ACCESS_KEY=minioadmin
export STORAGE_MINIO_SECRET_KEY=minioadmin

java -jar data-lake-receiver-1.0-SNAPSHOT.jar
```

---

## API Endpoints

### Upload File

The application supports **three ways** to specify the filename:

#### 1. Using Request Path (Recommended)

```bash
# Upload to specific filename using path
curl -X POST http://localhost:4000/myfile.txt \
  --data-binary @localfile.txt

# Upload with subdirectory structure
curl -X POST http://localhost:4000/documents/report.pdf \
  --data-binary @report.pdf

# Filename resolution from path
curl -X POST http://localhost:4000/logs/2024/app.log \
  --data-binary @app.log
```

#### 2. Using X-file-name Header

```bash
# Upload with custom filename via header
curl -X POST http://localhost:4000/ \
  -H "X-file-name: myfile.txt" \
  --data-binary @localfile.txt
```

#### 3. Auto-generated Filename

```bash
# Upload without filename (auto-generated timestamp-based name)
curl -X POST http://localhost:4000/ \
  --data-binary @localfile.txt
```

#### Filename Resolution Priority

The application resolves filenames with the following priority:
1. **Request path** - If POST is made to a specific path (e.g., `/a.txt`, `/dir/b.json`)
2. **X-file-name header** - If provided and path is `/`
3. **Auto-generated** - Timestamp-based name (e.g., `1234567890.data`)

**Examples:**

```bash
# Priority 1: Path wins
curl -X POST http://localhost:4000/documents/report.pdf \
  -H "X-file-name: ignored.txt" \
  --data-binary @report.pdf
# Stores as: documents/report.pdf

# Priority 2: Header used when path is /
curl -X POST http://localhost:4000/ \
  -H "X-file-name: myfile.txt" \
  --data-binary @localfile.txt
# Stores as: myfile.txt

# Priority 3: Auto-generated when neither specified
curl -X POST http://localhost:4000/ \
  --data-binary @localfile.txt
# Stores as: 1731831600000.data
```

**Response:**
```
File stored successfully: myfile.txt
```

### Health Check

```bash
curl http://localhost:4000/health
```

**Response:**
```
OK - Storage: FileSystem [files]
```

---

## Configuration

### Configuration Sources

The application uses a layered configuration approach with the following **priority order** (highest to lowest):

1. **Command Line Arguments** - Highest priority
2. **Environment Variables** - Override YAML configuration
3. **application.yml** - Default configuration file
4. **Code Defaults** - Fallback values

This means you can set defaults in `application.yml` and override them with environment variables in production without modifying the JAR file.

### Configuration Properties

#### Server Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `server.port` | Integer | `4000` | HTTP server port |

#### Storage Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `storage.type` | Enum | `filesystem` | Storage backend: `FILESYSTEM` or `MINIO` |

#### Filesystem Storage

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `storage.filesystem.directory` | String | `files` | Directory path where files will be stored |

#### MinIO Storage

| Property | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `storage.minio.endpoint` | String | - | Yes* | MinIO server URL (e.g., `http://localhost:9000`) |
| `storage.minio.access-key` | String | - | Yes* | MinIO access key for authentication |
| `storage.minio.secret-key` | String | - | Yes* | MinIO secret key for authentication |
| `storage.minio.bucket-name` | String | `data-lake` | No | Bucket name where files will be stored |

*Required when `storage.type=minio`

### application.yml

The main configuration file is `src/main/resources/application.yml`:

```yaml
server:
  port: 4000

spring:
  application:
    name: data-lake-receiver

storage:
  type: filesystem  # or minio

  filesystem:
    directory: files

  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: data-lake
```

### Spring Profiles

#### Default Profile
Uses filesystem storage with default settings.

```bash
java -jar data-lake-receiver-1.0-SNAPSHOT.jar
```

#### Production Profile
Reduces logging verbosity for production environments.

```bash
java -jar data-lake-receiver-1.0-SNAPSHOT.jar --spring.profiles.active=production
```

#### MinIO Profile
Automatically switches to MinIO storage backend.

```bash
java -jar data-lake-receiver-1.0-SNAPSHOT.jar --spring.profiles.active=minio
```

### Configuration Quick Examples

#### Option 1: Use application.yml Only (Easiest for Development)

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 4000
storage:
  type: filesystem
  filesystem:
    directory: ./data/files
```

Run:
```bash
java -jar data-lake-receiver-1.0-SNAPSHOT.jar
```

#### Option 2: Use Environment Variables Only (Best for Production)

```bash
export SERVER_PORT=8080
export STORAGE_TYPE=MINIO
export STORAGE_MINIO_ENDPOINT=http://localhost:9000
export STORAGE_MINIO_ACCESS_KEY=minioadmin
export STORAGE_MINIO_SECRET_KEY=minioadmin
export STORAGE_MINIO_BUCKET_NAME=production-data

java -jar data-lake-receiver-1.0-SNAPSHOT.jar
```

#### Option 3: Mix Both (Recommended)

**application.yml** (development defaults):
```yaml
server:
  port: 4000
storage:
  type: filesystem
  filesystem:
    directory: ./dev-files
```

**Production** (override with environment variables):
```bash
export STORAGE_TYPE=MINIO
export STORAGE_MINIO_ENDPOINT=https://minio.prod.com
export STORAGE_MINIO_ACCESS_KEY=${PROD_ACCESS_KEY}
export STORAGE_MINIO_SECRET_KEY=${PROD_SECRET_KEY}

java -jar data-lake-receiver-1.0-SNAPSHOT.jar
```

---

## Storage Backends

### Filesystem Storage Details

Stores files to the local filesystem directory.

#### Features
- **Simple** - No external dependencies
- **Fast** - Low latency for local operations
- **Easy Setup** - Just specify a directory path

#### Configuration

```yaml
storage:
  type: filesystem
  filesystem:
    directory: /data/uploads
```

Or via environment:
```bash
export STORAGE_TYPE=FILESYSTEM
export STORAGE_FILESYSTEM_DIRECTORY=/data/uploads
```

#### Docker Example

```bash
docker run -d \
  -p 4000:4000 \
  -e STORAGE_TYPE=FILESYSTEM \
  -e STORAGE_FILESYSTEM_DIRECTORY=/data/files \
  -v /host/path:/data/files \
  data-lake-receiver:latest
```

#### Pros and Cons

**Pros:**
- Low latency
- Simple deployment
- No external dependencies

**Cons:**
- Limited scalability
- Single point of failure
- Not suitable for distributed systems

### MinIO Storage Details

Stores files to a MinIO object storage bucket (S3-compatible).

#### Features
- **Scalable** - Distributed object storage
- **High Availability** - Built-in redundancy
- **S3-Compatible** - Works with any S3-compatible storage
- **Auto-Bucket Creation** - Creates bucket if it doesn't exist

#### Configuration

```yaml
storage:
  type: minio
  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: data-lake
```

Or via environment:
```bash
export STORAGE_TYPE=MINIO
export STORAGE_MINIO_ENDPOINT=http://localhost:9000
export STORAGE_MINIO_ACCESS_KEY=minioadmin
export STORAGE_MINIO_SECRET_KEY=minioadmin
export STORAGE_MINIO_BUCKET_NAME=data-lake
```

#### Docker Compose Example

```yaml
version: '3.8'

services:
  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      - MINIO_ROOT_USER=minioadmin
      - MINIO_ROOT_PASSWORD=minioadmin
    command: server /data --console-address ":9001"
    volumes:
      - minio-data:/data

  data-lake-receiver:
    image: data-lake-receiver:latest
    ports:
      - "4000:4000"
    environment:
      - STORAGE_TYPE=MINIO
      - STORAGE_MINIO_ENDPOINT=http://minio:9000
      - STORAGE_MINIO_ACCESS_KEY=minioadmin
      - STORAGE_MINIO_SECRET_KEY=minioadmin
      - STORAGE_MINIO_BUCKET_NAME=data-lake
    depends_on:
      - minio

volumes:
  minio-data:
```

Run with:
```bash
docker-compose up -d
```

#### Pros and Cons

**Pros:**
- Scalable and distributed
- S3-compatible
- High availability
- Suitable for production/cloud deployments

**Cons:**
- Network overhead
- Requires additional infrastructure
- More complex setup

#### Security Considerations

- Use strong access keys and secret keys
- Enable HTTPS/TLS for production deployments
- Implement bucket policies and access controls
- Rotate credentials regularly
- Use network policies to restrict MinIO access

---

## Startup Banner

### Custom Banner

The application displays a custom ASCII art banner on startup with dynamic information:

```
  ____        _          _           _          ____               _
 |  _ \  __ _| |_ __ _  | |    __ _ | | _____  |  _ \ ___  ___ ___(_)_   _____ _ __
 | | | |/ _` | __/ _` | | |   / _` || |/ / _ \ | |_) / _ \/ __/ _ \ \ \ / / _ \ '__|
 | |_| | (_| | || (_| | | |__| (_| ||   <  __/ |  _ <  __/ (_|  __/ |\ V /  __/ |
 |____/ \__,_|\__\__,_| |_____\__,_||_|\_\___| |_| \_\___|\___\___|_| \_/ \___|_|

 :: Spring Boot 3.2.0 ::
 :: Application: Data Lake Receiver ::
 :: Version: 1.0-SNAPSHOT ::
 :: Storage: FILESYSTEM ::

 ===================================================================================
 Ready to receive and store files - Send POST requests to http://localhost:4000/
 ===================================================================================
```

### Banner Customization

#### Disable Banner

```bash
java -jar data-lake-receiver-1.0-SNAPSHOT.jar --spring.main.banner-mode=off
```

Or in `application.yml`:
```yaml
spring:
  main:
    banner-mode: off
```

#### Add Colors

Edit `src/main/resources/banner.txt`:

```
${AnsiColor.BRIGHT_CYAN}
  ____        _          _           _          ____               _
 |  _ \  __ _| |_ __ _  | |    __ _ | | _____  |  _ \ ___  ___ ___(_)_   _____ _ __
${AnsiColor.DEFAULT}
${AnsiColor.BRIGHT_GREEN} :: Spring Boot ${spring-boot.version} ::${AnsiColor.DEFAULT}
```

#### Available Placeholders

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `${spring-boot.version}` | Spring Boot version | 3.2.0 |
| `${application.title}` | Application title | Data Lake Receiver |
| `${application.version}` | Application version | 1.0-SNAPSHOT |
| `${server.port}` | Server port | 4000 |
| `${storage.type}` | Storage backend type | FILESYSTEM |

---

## Building from Source

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Build

```bash
mvn clean package
```

The executable JAR will be created at `target/data-lake-receiver-1.0-SNAPSHOT.jar`.

### Run Tests

```bash
mvn test
```

---

## Docker Deployment

### Using Pre-built Images (Recommended)

Pre-built images are automatically published to GitHub Container Registry on every release:

```bash
# Pull and run latest version
docker pull ghcr.io/sparkworks/data-lake-receiver:latest
docker run -p 4000:4000 ghcr.io/sparkworks/data-lake-receiver:latest

# Or use in docker-compose
```

Update `docker-compose.yml` to use the pre-built image:
```yaml
services:
  data-lake-receiver:
    image: ghcr.io/sparkworks/data-lake-receiver:latest  # Use pre-built image
    ports:
      - "4000:4000"
```

**Available images:**
- `ghcr.io/sparkworks/data-lake-receiver:latest` - Latest stable release
- `ghcr.io/sparkworks/data-lake-receiver:1.0.0` - Specific version
- `ghcr.io/sparkworks/data-lake-receiver:develop` - Development branch

See [GITHUB_REGISTRY.md](GITHUB_REGISTRY.md) for complete details on pulling images and authentication.

### Building Locally (Optional)

If you need to build the image locally:

```bash
# Build using traditional Dockerfile
docker build -t data-lake-receiver:latest .

# Or using Spring Boot Maven plugin
mvn spring-boot:build-image
```

### Option 1: Filesystem Storage (Simple)

```bash
# Start the service
docker-compose -f docker-compose-filesystem.yml up -d
```

**What you get:**
- Data Lake Receiver on port 4000
- Files stored in `./data/files/`

### Option 2: MinIO Storage (Production-Ready)

```bash
# Start services (MinIO + Data Lake Receiver)
docker-compose -f docker-compose-minio.yml up -d
```

**What you get:**
- MinIO server on ports 9000 (API) and 9001 (Console)
- Data Lake Receiver on port 4000
- Files stored in MinIO bucket
- MinIO Console at http://localhost:9001 (login: minioadmin/minioadmin)

### Quick Commands

```bash
# Build the image first
mvn spring-boot:build-image

# Start the service
docker-compose -f docker-compose-filesystem.yml up -d

# View logs
docker-compose -f docker-compose-filesystem.yml logs -f

# Stop and remove
docker-compose -f docker-compose-filesystem.yml down

# Upload a file
curl -X POST http://localhost:4000/ \
  -H "X-file-name: test.txt" \
  --data-binary @myfile.txt
```

### Detailed Guide

See [DOCKER.md](DOCKER.md) for:
- Complete setup instructions
- Configuration options
- Troubleshooting guide
- Production deployment tips
- Networking architecture

---

## Development

### Project Structure

```
src/main/java/net/sparkworks/ac3/logger/
├── DataLakeReceiverApplication.java      # Spring Boot main class
├── config/
│   ├── StorageConfiguration.java         # Bean configuration
│   └── StorageProperties.java            # Configuration properties
├── controller/
│   └── FileReceiverController.java       # REST controller
└── storage/
    ├── StorageProvider.java              # Interface
    ├── FileSystemStorageProvider.java    # Filesystem implementation
    └── MinIOStorageProvider.java         # MinIO implementation
```

### Adding Custom Storage Providers

1. **Create a class implementing `StorageProvider`**

```java
public class S3StorageProvider implements StorageProvider {
    @Override
    public void initialize() throws IOException {
        // Initialize S3 client
    }

    @Override
    public void store(String filename, byte[] data) throws IOException {
        // Upload to S3
    }

    @Override
    public String getName() {
        return "AWS S3";
    }
}
```

2. **Add configuration to `StorageProperties`**

```java
@Data
public static class S3Config {
    private String bucket;
    private String region;
    private String accessKey;
    private String secretKey;
}
```

3. **Update `StorageConfiguration` to create the bean**

```java
case S3 -> createS3Provider(properties.getS3());
```

4. **Add new storage type to the enum**

```java
public enum StorageType {
    FILESYSTEM,
    MINIO,
    S3
}
```

---

## Logging

### Configuration

Logging is configured in `application.yml`:

```yaml
logging:
  level:
    root: INFO
    net.sparkworks.ac3.logger: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

### Change Log Levels

Via environment variables:
```bash
export LOGGING_LEVEL_ROOT=WARN
export LOGGING_LEVEL_NET_SPARKWORKS_AC3_LOGGER=DEBUG
```

Via command line:
```bash
java -jar data-lake-receiver-1.0-SNAPSHOT.jar \
  --logging.level.root=WARN \
  --logging.level.net.sparkworks.ac3.logger=DEBUG
```

### Log Output

The application logs:
- Configuration on startup
- Storage provider initialization
- File upload requests with headers
- Storage operations (success/failure)
- Any errors with stack traces

Example output:
```
2025-11-16 15:45:23 - Starting DataLakeReceiverApplication...
2025-11-16 15:45:24 - Creating storage provider of type: FILESYSTEM
2025-11-16 15:45:24 - FileSystem storage initialized at: files
2025-11-16 15:45:25 - Started DataLakeReceiverApplication in 2.543 seconds
2025-11-16 15:45:30 - header[X-file-name]=[test.txt]
2025-11-16 15:45:30 - Storing file with filename: test.txt (1024 bytes)
2025-11-16 15:45:30 - Successfully stored file: test.txt (1024 bytes)
```

---

## Monitoring and Health

### Health Endpoint

```bash
curl http://localhost:4000/health
```

Response shows storage backend status:
```
OK - Storage: FileSystem [files]
```

Or for MinIO:
```
OK - Storage: MinIO [bucket: data-lake]
```

### Spring Boot Actuator

You can enable additional actuator endpoints by adding to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

And configuring in `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

## Troubleshooting

### Application Won't Start

**Check logs for configuration errors:**
```bash
java -jar data-lake-receiver-1.0-SNAPSHOT.jar --debug
```

**Common issues:**
- Invalid MinIO credentials
- Missing required configuration
- Port already in use

### Files Not Being Stored

**Filesystem Storage:**
1. Check directory permissions
2. Verify directory path exists or can be created
3. Check available disk space

**MinIO Storage:**
1. Verify MinIO server is running and accessible
2. Check endpoint URL is correct
3. Verify access key and secret key are correct
4. Check network connectivity

### Port Already in Use

Change the port:
```bash
java -jar data-lake-receiver-1.0-SNAPSHOT.jar --server.port=8080
```

Or set environment variable:
```bash
export SERVER_PORT=8080
```

### Configuration Not Working

**Check environment variables:**
```bash
printenv | grep -E '(SERVER_PORT|STORAGE)'
```

**Verify YAML syntax:**
- Use spaces, not tabs
- Check indentation
- Ensure property names match exactly

**Enable debug logging:**
```bash
java -jar data-lake-receiver-1.0-SNAPSHOT.jar \
  --logging.level.org.springframework.boot=DEBUG
```

### MinIO Connection Issues

**Test MinIO connectivity:**
```bash
curl http://localhost:9000/minio/health/live
```

**Check MinIO logs:**
```bash
docker logs minio-container-name
```

**Verify credentials:**
```bash
# Try accessing MinIO console
open http://localhost:9001
```

---

## Migration from Non-Spring Version

The Spring Boot version maintains API compatibility with the previous version.

### API Compatibility

✅ **Same Endpoints:**
- POST `/` - Upload files
- Same `X-file-name` header
- Same response behavior

### Configuration Migration

**Old (environment variables):**
```bash
export HTTP_SERVER_PORT=4000
export STORAGE_TYPE=filesystem
export FILES_DIR=files
```

**New (recommended):**
```bash
export SERVER_PORT=4000
export STORAGE_TYPE=FILESYSTEM
export STORAGE_FILESYSTEM_DIRECTORY=files
```

**Old (application.properties):**
```properties
HTTP_SERVER_PORT=4000
STORAGE_TYPE=filesystem
FILES_DIR=files
```

**New (application.yml):**
```yaml
server:
  port: 4000
storage:
  type: filesystem
  filesystem:
    directory: files
```

### Key Differences

| Feature | Old Version | New Version |
|---------|-------------|-------------|
| Web Server | `com.sun.httpserver` | Spring Boot / Embedded Tomcat |
| Configuration | Custom loader | Spring Boot Configuration |
| DI | Manual | Spring Framework |
| API | HTTP Handler | Spring REST Controller |
| Monitoring | None | Spring Actuator |
| Logging | Logback | Logback + Spring Boot |

---

## License

Apache License 2.0

---

## Support

For issues, questions, or contributions, please refer to the project repository.