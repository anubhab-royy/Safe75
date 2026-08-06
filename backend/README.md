# Safe75 Attendance Tracker Backend

The backend system for Safe75 serves as an offline-first bug report dynamic proxy gateway. It provides anonymous device enrollments protected by Proof-of-Work checks, signs and validates incoming requests using HMAC-SHA256 signatures, buffers screenshots, and saves report data to MongoDB Atlas and binary objects to Cloudflare R2 storage.

---

## 1. Architecture Summary
The backend acts as a streaming proxy between the Safe75 Android application and the cloud storage layers (MongoDB & Cloudflare R2). The Android client never communicates directly with cloud services.
*   **API Framework**: built with Ktor 3.x using Netty.
*   **Database Layer**: MongoDB Atlas for metadata persistence (via MongoDB Kotlin Coroutine driver).
*   **Storage Layer**: Cloudflare R2 for screenshot files (via AWS S3 Java SDK).
*   **Security Layer**: Custom Ktor route-scoped plugin interceptor validating rate-limits, replay nonces, clock drift, and request signatures.

---

## 2. Folder Structure
```
backend/
├── src/
│   ├── main/
│   │   ├── kotlin/com/attendance/tracker/backend/
│   │   │   ├── Application.kt            # Ktor Main server configuration
│   │   │   ├── config/
│   │   │   │   └── EnvironmentConfig.kt  # Configuration parser
│   │   │   ├── database/
│   │   │   │   └── MongoClientProvider.kt# MongoDB Client initialization
│   │   │   ├── routes/
│   │   │   │   └── HealthRoute.kt        # Service health checks
│   │   │   ├── storage/
│   │   │   │   ├── R2ClientProvider.kt   # Cloudflare R2 S3 provider
│   │   │   │   └── R2StorageService.kt   # R2 storage abstraction
│   │   │   ├── util/
│   │   │   │   ├── Uuid7Generator.kt     # Time-ordered UUID generator
│   │   │   │   └── ImageProcessor.kt     # Image resizer and metadata stripper
│   │   │   ├── security/
│   │   │   │   ├── ChallengeManager.kt   # PoW challenge manager
│   │   │   │   ├── RateLimiter.kt        # Progressive token-bucket rate limiting
│   │   │   │   ├── NonceCache.kt         # Nonce sliding replay protection
│   │   │   │   ├── SecurityMiddleware.kt # Security interceptor middleware
│   │   │   │   ├── routes/
│   │   │   │   │   └── SecurityRoutes.kt # PoW challenge & Enroll endpoints
│   │   │   │   ├── model/
│   │   │   │   │   └── SecurityModels.kt # Security schemas & status enums
│   │   │   │   └── repository/
│   │   │   │       └── DeviceRepository.kt# Device MongoDB repository
│   │   │   └── feature/reports/
│   │   │       ├── model/
│   │   │       │   ├── BugReportModels.kt# Bug report schemas
│   │   │       │   └── BugReportDtos.kt  # REST request/response structures
│   │   │       ├── repository/
│   │   │       │   └── BugReportRepository.kt# Report MongoDB repository
│   │   │       ├── service/
│   │   │       │   └── BugReportService.kt# Orchestration layer
│   │   │       ├── routes/
│   │   │       │   └── BugReportRoutes.kt# REST endpoints
│   │   │       └── validation/
│   │   │           └── BugReportValidator.kt# Input field validations
│   │   └── resources/
│   │       └── logback.xml               # SLF4J log configuration
│   └── test/
│       └── kotlin/com/attendance/tracker/backend/
│           ├── BugReportTests.kt         # Metadata validations & image processing tests
│           ├── SecurityTests.kt          # PoW, replay, rate limiting, and signature tests
│           └── IntegrationTests.kt       # Complete flow integration and load tests
└── build.gradle.kts                      # Build configurations & dependencies
```

---

## 3. Environment Setup & Configuration Reference
Create a `.env` file in the root directory (or parent directory) containing:

```env
# MongoDB Atlas Configuration
MONGODB_URI=mongodb+srv://<user>:<password>@cluster.mongodb.net
DATABASE_NAME=bug_reports

# Cloudflare R2 Configuration
R2_BUCKET=safe75-bug-reports
R2_ACCOUNT_ID=your_cloudflare_account_id
R2_ENDPOINT=https://your_cloudflare_account_id.r2.cloudflarestorage.com
R2_ACCESS_KEY=your_r2_access_key
R2_SECRET_KEY=your_r2_secret_key

# Security Configuration (Optional - Defaults apply if omitted)
POW_DIFFICULTY=4                          # Leading hex zeros required for enrollment (default 4)
RATE_LIMIT_IP_MAX=30                      # Max calls allowed per IP in window (default 30)
RATE_LIMIT_DEVICE_MAX=10                  # Max calls allowed per Device in window (default 10)
RATE_LIMIT_REFILL_PERIOD_MS=2000          # Milliseconds to refill 1 token in bucket (default 2s)
SECURITY_TIMESTAMP_DRIFT_LIMIT_MS=300000  # Max request clock drift in ms (default 5 minutes)
SECURITY_NONCE_TTL_MS=600000              # Nonce cache validity window in ms (default 10 minutes)
```

---

## 4. Running Locally
To launch the backend application locally:
```bash
# Compile and run Netty server
./gradlew :backend:run
```
The server will boot and bind to `http://localhost:8080` (or the port defined in configuration files).

---

## 5. Running Tests
To run unit and integration tests:
```bash
./gradlew :backend:test
```
This runs 100% of unit tests and integration tests including rate limiter load simulations.

---

## 6. API Overview

### 6.1 Service Health
*   **Endpoint**: `GET /health`
*   **Response**: `200 OK` if connections to MongoDB and Cloudflare R2 are active.

### 6.2 Anonymous Device Enrollment
Dynamic registration requires solving a Proof-of-Work puzzle to deter enrollment abuse:
1.  **Request Challenge**: `GET /api/v1/device/challenge`
    *   Returns: `{ "seed": "<hex_seed>", "difficulty": 4 }`
2.  **Solve Puzzle**: Loops nonces until `SHA-256(seed + nonce)` starts with the required difficulty (leading zeros).
3.  **Enroll**: `POST /api/v1/device/enroll`
    *   Body: `{ "seed": "<hex_seed>", "nonce": <solved_nonce> }`
    *   Returns: `{ "deviceId": "dev_...", "deviceSecret": "sec_...", "createdAt": <timestamp> }`

### 6.3 Secure Report Metadata Submission
*   **Endpoint**: `POST /api/v1/reports`
*   **Headers**:
    *   `X-Safe75-DeviceId`: `dev_...`
    *   `X-Safe75-Signature`: Calculated HMAC-SHA256 signature of `body + nonce + timestamp` using `deviceSecret`.
    *   `X-Safe75-Nonce`: Unique string nonce.
    *   `X-Safe75-Timestamp`: Submission timestamp (Long).
*   **Response**: `201 Created` returning the generated time-ordered UUIDv7 `reportId`.

### 6.4 Secure Screenshot Upload
*   **Endpoint**: `POST /api/v1/reports/{reportId}/screenshot`
*   **Body**: `multipart/form-data` with field `screenshot` containing image bytes (JPEG, PNG, WEBP; max 2MB).
*   **Headers**: Security signature computed over raw multipart payload + nonce + timestamp.
*   **Response**: `200 OK` returning R2 `objectKey`.

---

## 7. Deployment Guide
1.  **Containerization**: Build a Docker container using a standard JDK 17 slim base image.
2.  **Execution Jar**: Build fat/shadow distributions:
    ```bash
    ./gradlew :backend:build
    ```
3.  **Configuration**: Map all environment keys (`MONGODB_URI`, `R2_ACCESS_KEY`, etc.) inside your target staging/production platform environment (e.g. AWS ECS, GCP Cloud Run, or Kubernetes Secrets).
