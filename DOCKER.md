# Docker Deployment Guide

This guide explains how to run the Data Lake Receiver using Docker Compose with either filesystem or MinIO storage.

## Available Docker Compose Files

1. **`docker-compose-filesystem.yml`** - Filesystem storage (simple setup)
2. **`docker-compose-minio.yml`** - MinIO object storage (production-ready)

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 512MB RAM available
- For MinIO: At least 1GB RAM recommended

---

## Option 1: Filesystem Storage

### Quick Start

```bash
# Build the Docker image first
mvn spring-boot:build-image

# Start the service
docker-compose -f docker-compose-filesystem.yml up -d

# View logs
docker-compose -f docker-compose-filesystem.yml logs -f

# Stop
docker-compose -f docker-compose-filesystem.yml down
```

### What Gets Deployed

- **Data Lake Receiver** on port `4000`
- Files stored in `./data/files` directory on your host

### Directory Structure

```
data-lake-receiver/
├── data/
│   └── files/          # Your uploaded files appear here
├── docker-compose-filesystem.yml
└── Dockerfile
```

### Upload a File

```bash
curl -X POST http://localhost:4000/ \
  -H "X-file-name: test.txt" \
  --data-binary @myfile.txt
```

### Check Health

```bash
curl http://localhost:4000/health
```

Expected response:
```
OK - Storage: FileSystem [/app/data/files]
```

### Access Files

Files are stored in `./data/files/` on your host machine:

```bash
ls -la ./data/files/
```

### Configuration

Edit `docker-compose-filesystem.yml` to customize:

```yaml
environment:
  - SERVER_PORT=4000                              # Change port
  - STORAGE_FILESYSTEM_DIRECTORY=/app/data/files # Change path
  - LOGGING_LEVEL_ROOT=INFO                       # Change log level

logging:
  driver: "json-file"
  options:
    max-size: "5m"   # Maximum log file size
    max-file: "1"    # Keep only 1 log file
```

### Cleanup

```bash
# Stop and remove containers
docker-compose -f docker-compose-filesystem.yml down

# Remove stored files (optional)
rm -rf ./data/files/*
```

---

## Option 2: MinIO Storage

### Quick Start

```bash
# Build the Docker image first
mvn spring-boot:build-image

# Start services (MinIO + Data Lake Receiver)
docker-compose -f docker-compose-minio.yml up -d

# View logs
docker-compose -f docker-compose-minio.yml logs -f

# Stop
docker-compose -f docker-compose-minio.yml down
```

### What Gets Deployed

- **MinIO Server** on ports `9000` (API) and `9001` (Console)
- **Data Lake Receiver** on port `4000`
- Files stored in MinIO bucket named `data-lake`

### Access MinIO Console

Open your browser to: [http://localhost:9001](http://localhost:9001)

**Credentials:**
- Username: `minioadmin`
- Password: `minioadmin`

### Upload a File

```bash
curl -X POST http://localhost:4000/ \
  -H "X-file-name: test.txt" \
  --data-binary @myfile.txt
```

### Check Health

```bash
# Check Data Lake Receiver
curl http://localhost:4000/health

# Check MinIO
curl http://localhost:9000/minio/health/live
```

### View Files in MinIO

1. Open MinIO Console: http://localhost:9001
2. Login with `minioadmin` / `minioadmin`
3. Navigate to `data-lake` bucket
4. See your uploaded files

### Configuration

Edit `docker-compose-minio.yml` to customize:

```yaml
# MinIO Configuration
environment:
  - MINIO_ROOT_USER=minioadmin        # Change username
  - MINIO_ROOT_PASSWORD=minioadmin    # Change password

# Data Lake Receiver Configuration
environment:
  - STORAGE_MINIO_BUCKET_NAME=data-lake      # Change bucket name
  - STORAGE_MINIO_ACCESS_KEY=minioadmin      # Match MinIO user
  - STORAGE_MINIO_SECRET_KEY=minioadmin      # Match MinIO password
```

### Cleanup

```bash
# Stop and remove containers
docker-compose -f docker-compose-minio.yml down

# Remove MinIO data (optional - WARNING: deletes all files)
docker-compose -f docker-compose-minio.yml down -v
```

---

## Building the Docker Image

The Data Lake Receiver uses **Spring Boot Maven Plugin** with Cloud Native Buildpacks to create optimized Docker images. This approach provides:

- Layered JARs for better caching
- Automatic base image updates
- Security best practices
- No Dockerfile needed

### Build Using Maven (Recommended)

```bash
# Build the Docker image using Spring Boot buildpacks
mvn spring-boot:build-image

# This creates: data-lake-receiver:1.0-SNAPSHOT and data-lake-receiver:latest
```

The generated image includes **minimal essential defaults** configured in `pom.xml`:
- `SPRING_PROFILES_ACTIVE=default`
- `SERVER_PORT=4000`
- `STORAGE_TYPE=FILESYSTEM`
- JVM memory settings optimized for containers

**All other configuration** (MinIO settings, logging, etc.) comes from:
1. `application.yml` defaults (baked into JAR)
2. Runtime environment variables (override defaults)

Spring Boot **automatically translates** environment variables to properties:
```bash
# No special prefix needed at runtime!
docker run -e STORAGE_MINIO_ENDPOINT=http://minio:9000 data-lake-receiver:latest
# → Becomes: storage.minio.endpoint=http://minio:9000
```

### Build with Custom Image Name

```bash
# Specify custom image name
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=myregistry/data-lake-receiver:v1.0
```

### Build Options

```bash
# Skip tests and build image
mvn clean package -DskipTests spring-boot:build-image

# Build with specific builder
mvn spring-boot:build-image -Dspring-boot.build-image.builder=paketobuildpacks/builder-jammy-tiny:latest
```

### Environment Variables Configuration

#### Build-time Defaults (Baked into Image)

Only essential defaults are configured in `pom.xml` using the `BPE_` prefix:

| Variable | Default Value | Source | Override |
|----------|--------------|--------|----------|
| `SPRING_PROFILES_ACTIVE` | `default` | pom.xml | Runtime env var |
| `SERVER_PORT` | `4000` | pom.xml | Runtime env var |
| `STORAGE_TYPE` | `FILESYSTEM` | pom.xml | Runtime env var |
| `JAVA_TOOL_OPTIONS` | JVM optimizations | pom.xml | Runtime env var |

#### Runtime Configuration (From application.yml + Environment Variables)

All other configuration uses **application.yml defaults** that can be overridden at runtime:

| Variable | Default (application.yml) | Description |
|----------|--------------------------|-------------|
| `STORAGE_FILESYSTEM_DIRECTORY` | `files` | Directory path for file storage |
| `STORAGE_MINIO_ENDPOINT` | `http://localhost:9000` | MinIO server URL |
| `STORAGE_MINIO_ACCESS_KEY` | `minioadmin` | MinIO access key |
| `STORAGE_MINIO_SECRET_KEY` | `minioadmin` | MinIO secret key |
| `STORAGE_MINIO_BUCKET_NAME` | `data-lake` | MinIO bucket name |
| `LOGGING_LEVEL_ROOT` | `INFO` | Root logging level |
| `LOGGING_LEVEL_NET_SPARKWORKS_AC3_LOGGER` | `DEBUG` | Application logging level |

#### How Environment Variables Work

**Spring Boot automatically translates environment variables:**
```bash
STORAGE_MINIO_ENDPOINT     → storage.minio.endpoint
SERVER_PORT                → server.port
LOGGING_LEVEL_ROOT         → logging.level.root
```

**Configuration priority (highest to lowest):**
1. **Runtime environment variables** (docker run -e, docker-compose environment)
2. **Build-time environment variables** (pom.xml BPE_ variables)
3. **application.yml** (baked into JAR)

**Example - Override at runtime:**
```bash
# No special prefix needed!
docker run -e STORAGE_TYPE=MINIO \
           -e STORAGE_MINIO_ENDPOINT=http://minio:9000 \
           -e STORAGE_MINIO_ACCESS_KEY=your-key \
           -e STORAGE_MINIO_SECRET_KEY=your-secret \
           data-lake-receiver:latest
```

**Example - Docker Compose:**
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=production
  - STORAGE_TYPE=MINIO
  - STORAGE_MINIO_ENDPOINT=http://minio:9000
  - STORAGE_MINIO_ACCESS_KEY=${PROD_ACCESS_KEY}
  - LOGGING_LEVEL_ROOT=WARN
```

**To customize build-time defaults** (rarely needed), edit `pom.xml`:
```xml
<env>
    <BPE_SPRING_PROFILES_ACTIVE>production</BPE_SPRING_PROFILES_ACTIVE>
    <BPE_SERVER_PORT>8080</BPE_SERVER_PORT>
</env>
```

### Traditional Dockerfile (Alternative)

If you prefer using a traditional Dockerfile, one is provided:

```bash
docker build -t data-lake-receiver:latest .
```

---

## Advanced Usage

### Custom Environment Variables

Create a `.env` file:

```env
# Server
SERVER_PORT=8080

# MinIO
MINIO_ROOT_USER=myadmin
MINIO_ROOT_PASSWORD=secretpassword
STORAGE_MINIO_BUCKET_NAME=production-data

# Logging
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_NET_SPARKWORKS_AC3_LOGGER=INFO
```

Use with docker-compose:

```bash
docker-compose -f docker-compose-minio.yml --env-file .env up -d
```

### Production Deployment

For production, consider:

1. **Use secrets** instead of environment variables:

```yaml
secrets:
  minio_access_key:
    file: ./secrets/minio_access_key.txt
  minio_secret_key:
    file: ./secrets/minio_secret_key.txt
```

2. **Enable TLS** for MinIO

3. **Use external volumes** for persistence:

```yaml
volumes:
  minio-data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/storage/minio
```

4. **Add reverse proxy** (Nginx/Traefik) for HTTPS

5. **Configure resource limits**:

```yaml
deploy:
  resources:
    limits:
      cpus: '2'
      memory: 2G
    reservations:
      cpus: '1'
      memory: 512M
```

### Monitoring

#### View Logs

```bash
# All services
docker-compose -f docker-compose-minio.yml logs -f

# Specific service
docker-compose -f docker-compose-minio.yml logs -f data-lake-receiver
docker-compose -f docker-compose-minio.yml logs -f minio

# View last 100 lines
docker-compose -f docker-compose-minio.yml logs --tail=100 data-lake-receiver
```

#### Log Management

Both Docker Compose files are configured with log rotation to prevent disk space issues:

```yaml
logging:
  driver: "json-file"
  options:
    max-size: "5m"   # Maximum 5MB per log file
    max-file: "1"    # Keep only 1 log file (no rotation)
```

**What this means:**
- Each container's logs are limited to **5MB maximum**
- When the log file reaches 5MB, old entries are **overwritten**
- Only **1 log file** is kept (no historical log files)
- Total disk usage per container: **~5MB**

**To view container logs directly:**
```bash
# Find container log files
docker inspect --format='{{.LogPath}}' data-lake-receiver-minio

# View raw log file
docker logs data-lake-receiver-minio --tail 100
```

**To change log limits**, edit the compose file:
```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"   # Increase to 10MB
    max-file: "3"     # Keep 3 rotated files (30MB total)
```

#### Container Stats

```bash
docker stats
```

#### Health Status

```bash
docker-compose -f docker-compose-minio.yml ps
```

---

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker-compose -f docker-compose-minio.yml logs

# Check container status
docker-compose -f docker-compose-minio.yml ps

# Rebuild image
docker-compose -f docker-compose-minio.yml build --no-cache
```

### Permission Issues (Filesystem)

```bash
# Fix permissions on host directory
chmod -R 777 ./data/files

# Or set proper ownership
sudo chown -R 1000:1000 ./data/files
```

### MinIO Connection Errors

```bash
# Verify MinIO is running
docker-compose -f docker-compose-minio.yml ps minio

# Check MinIO logs
docker-compose -f docker-compose-minio.yml logs minio

# Test MinIO connectivity from receiver container
docker-compose -f docker-compose-minio.yml exec data-lake-receiver \
  curl -v http://minio:9000/minio/health/live
```

### Port Conflicts

Change ports in docker-compose file:

```yaml
ports:
  - "8080:4000"  # Map host port 8080 to container port 4000
```

### Reset Everything

```bash
# Stop and remove everything
docker-compose -f docker-compose-minio.yml down -v

# Remove images
docker rmi data-lake-receiver:latest

# Start fresh
docker-compose -f docker-compose-minio.yml up -d --build
```

---

## Network Architecture

### Filesystem Setup

```
┌─────────────────────────────┐
│   Host Machine              │
│                             │
│  ┌─────────────────────┐   │
│  │ Data Lake Receiver  │   │
│  │  Port: 4000         │   │
│  └──────────┬──────────┘   │
│             │               │
│  ┌──────────▼──────────┐   │
│  │  ./data/files/      │   │
│  │  (Volume Mount)     │   │
│  └─────────────────────┘   │
└─────────────────────────────┘
```

### MinIO Setup

```
┌──────────────────────────────────────────────┐
│   Host Machine                               │
│                                              │
│  ┌─────────────────────┐  ┌──────────────┐ │
│  │ Data Lake Receiver  │  │    MinIO     │ │
│  │  Port: 4000         │──│  Port: 9000  │ │
│  └─────────────────────┘  │  Port: 9001  │ │
│                            └──────┬───────┘ │
│                                   │         │
│                            ┌──────▼───────┐ │
│                            │ MinIO Volume │ │
│                            │  (Persistent)│ │
│                            └──────────────┘ │
└──────────────────────────────────────────────┘
```

---

## FAQ

### Q: Why use Spring Boot buildpacks instead of Dockerfile?
A: Spring Boot buildpacks provide several advantages:
- **Layered JARs**: Better caching and faster deployments (dependencies in separate layers)
- **Automatic Updates**: Base images updated by Paketo buildpacks team
- **Security**: Regularly patched base images with minimal CVEs
- **Optimizations**: Built-in memory calculators and performance tuning
- **No Maintenance**: No need to maintain Dockerfile as Spring Boot versions change
- **Reproducible**: Consistent builds across different environments

The traditional Dockerfile is still provided as an alternative if you need custom build steps.

### Q: Can I use both compose files together?
A: No, use one or the other. They're alternatives, not meant to run simultaneously.

### Q: How do I persist data with filesystem storage?
A: Data is automatically persisted to `./data/files/` on your host.

### Q: How do I backup MinIO data?
A: Backup the Docker volume or use MinIO's mc client to mirror buckets.

```bash
# Backup volume
docker run --rm -v data-lake-receiver_minio-data:/data \
  -v $(pwd)/backup:/backup alpine \
  tar czf /backup/minio-backup.tar.gz /data
```

### Q: Can I change the MinIO credentials?
A: Yes, edit the `MINIO_ROOT_USER` and `MINIO_ROOT_PASSWORD` in docker-compose-minio.yml, and update the corresponding variables in the data-lake-receiver service.

### Q: How do I scale this for production?
A: Consider:
- Use Kubernetes for orchestration
- External load balancer
- Distributed MinIO setup
- Separate database for metadata
- Monitoring with Prometheus/Grafana

---

## Next Steps

- See [README.md](README.md) for application documentation
- Configure [application.yml](src/main/resources/application.yml) for customization
- Set up monitoring and alerting
- Implement backup strategy
- Configure HTTPS/TLS for production