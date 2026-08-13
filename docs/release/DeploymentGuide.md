# Safe75 Deployment Guide

## Backend

1. Provision MongoDB Atlas and a Cloudflare R2 bucket.
2. Configure `MONGODB_URI`, `DATABASE_NAME`, `R2_BUCKET`, `R2_ACCOUNT_ID`,
   `R2_ENDPOINT`, `R2_ACCESS_KEY`, and `R2_SECRET_KEY` through the deployment
   secret manager.
3. Set security values such as `POW_DIFFICULTY`, rate limits, timestamp drift,
   and nonce TTL explicitly for production.
4. Build and run the backend with JDK 17:
   ```bash
   ./gradlew :backend:build
   ./gradlew :backend:run
   ```
5. Verify `GET /health` reports healthy database and storage dependencies.

The backend persists report metadata in MongoDB and stores processed screenshots
under `reports/{reportId}/screenshot.jpg` in R2.

## Android

1. Create or securely provision `keystore.properties`; never commit it.
2. Set the deployed HTTPS API endpoint:
   ```bash
   SAFE75_API_BASE_URL=https://api.example.com/ ./gradlew :app:assembleRelease
   ```
3. Verify the release certificate with `apksigner verify --print-certs`.
4. Install the release APK on a physical device and validate enrollment,
   online reporting, airplane-mode queueing, recovery, restart, and reboot.

Without a real signing key, the Gradle build uses the debug key for local
validation only and is not suitable for Play distribution.
