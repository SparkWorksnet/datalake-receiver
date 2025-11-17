# GitHub Container Registry Deployment Guide

This guide explains how to build and publish Docker images to GitHub Container Registry (ghcr.io) for the Data Lake Receiver application.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Automated Deployment (GitHub Actions)](#automated-deployment-github-actions)
- [Manual Deployment](#manual-deployment)
  - [Linux/macOS](#linuxmacos)
  - [Windows](#windows)
- [Pulling Published Images](#pulling-published-images)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

---

## Overview

The Data Lake Receiver can be automatically published to GitHub Container Registry using:

1. **GitHub Actions** - Automated CI/CD on push/tag
2. **Manual Scripts** - For local development and testing

Published images are available at:
```
ghcr.io/sparkworks/data-lake-receiver:latest
ghcr.io/sparkworks/data-lake-receiver:1.0-SNAPSHOT
ghcr.io/sparkworks/data-lake-receiver:v1.0.0
```

---

## Prerequisites

### For GitHub Actions (Automatic)
- Repository hosted on GitHub
- GitHub Actions enabled (enabled by default)
- No additional setup needed - uses built-in `GITHUB_TOKEN`

### For Manual Push Scripts
- **Docker installed and running**
- **GitHub Personal Access Token** with `write:packages` permission
- **Maven** (if building locally)

### Creating a GitHub Personal Access Token

1. Go to [GitHub Settings > Tokens](https://github.com/settings/tokens)
2. Click "Generate new token (classic)"
3. Give it a descriptive name (e.g., "Docker Push Token")
4. Select scopes:
   - ✅ `write:packages` - Upload packages to GitHub Package Registry
   - ✅ `read:packages` - Download packages from GitHub Package Registry
   - ✅ `delete:packages` - Delete packages (optional)
5. Click "Generate token"
6. **Copy the token immediately** (you won't see it again!)
7. Save it securely

---

## Automated Deployment (GitHub Actions)

### How It Works

The GitHub Actions workflow (`.github/workflows/docker-publish.yml`) automatically:

1. **Builds** the Docker image when you push code
2. **Tags** images based on branch/tag
3. **Pushes** to GitHub Container Registry
4. **Creates** multi-architecture images (amd64 + arm64)

### Triggers

The workflow runs automatically on:

| Event | When | Image Tags Created |
|-------|------|-------------------|
| **Push to main/master** | Merge to default branch | `latest`, `main` |
| **Push to develop** | Push to develop branch | `develop` |
| **Git tag** | Create tag `v1.0.0` | `v1.0.0`, `1.0`, `1`, `latest` |
| **Pull Request** | Create/update PR | `pr-123` |
| **Manual** | Workflow dispatch | Current branch |

### Example Workflows

#### 1. Release a New Version

```bash
# Create and push a version tag
git tag v1.2.3
git push origin v1.2.3

# GitHub Actions will automatically build and publish:
# - ghcr.io/sparkworks/data-lake-receiver:v1.2.3
# - ghcr.io/sparkworks/data-lake-receiver:1.2
# - ghcr.io/sparkworks/data-lake-receiver:1
# - ghcr.io/sparkworks/data-lake-receiver:latest
```

#### 2. Push to Development

```bash
# Push to develop branch
git push origin develop

# GitHub Actions will automatically build and publish:
# - ghcr.io/sparkworks/data-lake-receiver:develop
```

#### 3. Manual Trigger

1. Go to repository on GitHub
2. Click **Actions** tab
3. Select **Build and Push Docker Image** workflow
4. Click **Run workflow**
5. Select branch
6. Click **Run workflow** button

### Viewing Build Status

1. Go to repository on GitHub
2. Click **Actions** tab
3. See workflow runs and logs

### Configuration

The workflow is configured in `.github/workflows/docker-publish.yml`:

```yaml
env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}  # sparkworks/data-lake-receiver
```

**To customize:**
- Change organization: Fork repository or change `IMAGE_NAME`
- Add more triggers: Edit `on:` section
- Modify platforms: Edit `platforms:` (amd64, arm64, arm/v7, etc.)

---

## Manual Deployment

### Linux/macOS

Use the `push-to-github.sh` script.

#### Setup

```bash
# Make script executable
chmod +x push-to-github.sh

# Set GitHub token
export GITHUB_TOKEN=ghp_xxxxxxxxxxxx
```

#### Usage

```bash
# Basic usage (auto-detects version from pom.xml)
./push-to-github.sh

# Specify version and user/org
./push-to-github.sh --user sparkworks --version 1.0.0

# Use Spring Boot buildpacks instead of Dockerfile
./push-to-github.sh --build-method buildpacks

# Pass token directly
./push-to-github.sh --token ghp_xxxxxxxxxxxx --version 1.2.3
```

#### Options

```
-t, --token TOKEN      GitHub Personal Access Token
-u, --user USERNAME    GitHub username or organization (default: sparkworks)
-v, --version VERSION  Version tag (default: auto-detected from pom.xml)
-b, --build-method     Build method: 'dockerfile' or 'buildpacks' (default: dockerfile)
-h, --help            Show help message
```

### Windows

Use the `push-to-github.bat` script.

#### Setup

```batch
REM Set GitHub token
set GITHUB_TOKEN=ghp_xxxxxxxxxxxx
```

#### Usage

```batch
REM Basic usage
push-to-github.bat

REM With arguments: [TOKEN] [USER] [VERSION]
push-to-github.bat ghp_xxxxxxxxxxxx sparkworks 1.0.0

REM Using environment variable
set GITHUB_TOKEN=ghp_xxxxxxxxxxxx
push-to-github.bat
```

---

## Pulling Published Images

### Public Images

If the repository is public, anyone can pull images:

```bash
# Pull latest version
docker pull ghcr.io/sparkworks/data-lake-receiver:latest

# Pull specific version
docker pull ghcr.io/sparkworks/data-lake-receiver:1.0.0

# Run the image
docker run -p 4000:4000 ghcr.io/sparkworks/data-lake-receiver:latest
```

### Private Images

For private repositories, authenticate first:

```bash
# Login to GitHub Container Registry
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin

# Pull the image
docker pull ghcr.io/sparkworks/data-lake-receiver:latest

# Logout (optional)
docker logout ghcr.io
```

### Using in Docker Compose

Update your `docker-compose.yml`:

```yaml
services:
  data-lake-receiver:
    image: ghcr.io/sparkworks/data-lake-receiver:latest
    ports:
      - "4000:4000"
    environment:
      - STORAGE_TYPE=FILESYSTEM
```

For private images, login first:
```bash
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
docker-compose up -d
```

---

## Configuration

### Making Images Public

By default, packages are private. To make them public:

1. Go to the package page:
   ```
   https://github.com/users/sparkworks/packages/container/data-lake-receiver
   ```
2. Click **Package settings**
3. Scroll to **Danger Zone**
4. Click **Change visibility**
5. Select **Public**
6. Confirm

### Package Permissions

To allow teams or users to access private images:

1. Go to package settings (link above)
2. Click **Manage Actions access**
3. Add repositories, teams, or users
4. Set permissions (read, write, admin)

### Image Retention

To automatically delete old images:

1. Go to package settings
2. Under **Package retention**
3. Configure retention rules:
   - Keep latest N versions
   - Delete images older than X days
   - Keep tagged versions

Example:
```
Delete untagged versions after 7 days
Keep 5 most recent tagged versions
```

---

## Troubleshooting

### Authentication Failed

**Error:**
```
Error response from daemon: Get "https://ghcr.io/v2/": unauthorized
```

**Solution:**
1. Check token has `write:packages` permission
2. Verify token is not expired
3. Re-login:
   ```bash
   docker logout ghcr.io
   echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
   ```

### Package Not Found

**Error:**
```
Error: ghcr.io/sparkworks/data-lake-receiver:latest: not found
```

**Solution:**
1. Check package exists: https://github.com/sparkworks?tab=packages
2. If private, ensure you're authenticated
3. Check image name matches repository name

### Permission Denied

**Error:**
```
denied: permission_denied: write_package
```

**Solution:**
1. Ensure you're using the correct GitHub username
2. For organization packages, you need organization member access
3. Check repository permissions
4. Regenerate token with correct permissions

### Build Fails in GitHub Actions

**Check workflow logs:**
1. Go to repository **Actions** tab
2. Click the failed workflow run
3. Expand failed step
4. Check error messages

**Common issues:**
- Maven build fails → Check `pom.xml` and dependencies
- Docker build fails → Check `Dockerfile` syntax
- Permission denied → Check repository settings and token

### Image Size Too Large

**Solutions:**

1. Use multi-stage builds (already implemented in Dockerfile)
2. Minimize layers in Dockerfile
3. Use `.dockerignore` to exclude files
4. Clean up in same RUN command:
   ```dockerfile
   RUN apt-get update && \
       apt-get install -y package && \
       apt-get clean && \
       rm -rf /var/lib/apt/lists/*
   ```

### Multi-Architecture Build Fails

If building for multiple platforms fails:

```bash
# Setup buildx
docker buildx create --use

# Build for specific platform
docker buildx build --platform linux/amd64 -t image:tag .
```

---

## Best Practices

1. **Use Semantic Versioning**
   - Tags: `v1.0.0`, `v1.1.0`, `v2.0.0`
   - GitHub Actions will create major/minor tags automatically

2. **Always Tag Releases**
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

3. **Keep Images Small**
   - Use Alpine base images
   - Multi-stage builds
   - Minimize layers

4. **Secure Tokens**
   - Never commit tokens to repository
   - Use environment variables
   - Rotate tokens periodically

5. **Test Before Release**
   - Test develop/feature branches first
   - Only tag main/master when stable

6. **Document Changes**
   - Update version in `pom.xml`
   - Add release notes
   - Update CHANGELOG

---

## Example CI/CD Workflow

### Development Process

```bash
# 1. Create feature branch
git checkout -b feature/new-feature

# 2. Make changes and commit
git add .
git commit -m "Add new feature"

# 3. Push to GitHub
git push origin feature/new-feature

# 4. Create Pull Request
# GitHub Actions builds PR image: ghcr.io/sparkworks/data-lake-receiver:pr-123

# 5. Merge to develop
# GitHub Actions builds: ghcr.io/sparkworks/data-lake-receiver:develop

# 6. Test develop branch image
docker pull ghcr.io/sparkworks/data-lake-receiver:develop
# Test thoroughly...

# 7. Merge to main
# GitHub Actions builds: ghcr.io/sparkworks/data-lake-receiver:main

# 8. Create release tag
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions builds:
# - ghcr.io/sparkworks/data-lake-receiver:v1.0.0
# - ghcr.io/sparkworks/data-lake-receiver:1.0
# - ghcr.io/sparkworks/data-lake-receiver:1
# - ghcr.io/sparkworks/data-lake-receiver:latest
```

---

## Additional Resources

- [GitHub Container Registry Documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Build Push Action](https://github.com/docker/build-push-action)
- [Docker Metadata Action](https://github.com/docker/metadata-action)

---

## Support

For issues with:
- **GitHub Actions**: Check repository Actions tab
- **Manual scripts**: Run with `--help` flag
- **Authentication**: Verify token permissions
- **Package visibility**: Check package settings on GitHub