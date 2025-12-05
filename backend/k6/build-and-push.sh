#!/bin/bash
set -e

# Build multi-platform k6 image and push to GitHub Container Registry

# Create and use a buildx builder that supports multi-platform builds
BUILDER_NAME="multiplatform-builder"

if ! docker buildx inspect "$BUILDER_NAME" > /dev/null 2>&1; then
  echo "Creating buildx builder: $BUILDER_NAME"
  docker buildx create --name "$BUILDER_NAME" --use
else
  echo "Using existing buildx builder: $BUILDER_NAME"
  docker buildx use "$BUILDER_NAME"
fi

echo "Building k6 image for linux/amd64 and linux/arm64..."

docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t ghcr.io/xbodnar/ss-k6:latest \
  --push \
  .

echo "Successfully built and pushed ghcr.io/xbodnar/ss-k6:latest"
