# Qeema Util

Qeema Util is a secure operations platform for document management, application identity and roles, transactional email, and searchable audit history.

The repository contains:

- A Spring Boot REST API secured as an OAuth 2.0 resource server with Keycloak-issued JWTs.
- A React 19/Vite administration console.
- MySQL persistence with Flyway migrations.
- Kafka-backed audit event publishing and consumption.
- S3-compatible object storage through Silo.
- OpenAPI documentation and an importable Postman collection.

## Features

| Area | Capabilities |
| --- | --- |
| Documents | Upload, list, download, soft-delete, and create document versions |
| Identity | Provision the authenticated user and expose the current application identity |
| Roles | List users and application roles, then assign or remove roles |
| Email | Send plain-text or template-based transactional email and retain delivery logs |
| Email templates | Create, update, list, preview, and delete templates |
| Audit trail | Search and inspect audit events by actor, action, entity, and date range |
| Security | JWT validation, local application roles, method-level authorization, CORS, and correlation IDs |
| Administration UI | Browser-based workflows for documents, email, audit history, and identity administration |

## Architecture

```text
                         ┌──────────────────────┐
                         │ Keycloak             │
                         │ OAuth 2.0 / OIDC     │
                         └──────────┬───────────┘
                                    │ JWT
┌──────────────────────┐            │
│ React/Vite console   │────────────┐
│ http://localhost:5173│            │
└──────────────────────┘            ▼
                         ┌──────────────────────┐
                         │ Spring Boot API      │
                         │ http://localhost:8080│
                         └───┬────────┬────┬────┘
                             │        │    │
                    ┌────────▼──┐ ┌─▼──┐ ┌▼─────────────┐
                    │ MySQL     │ │Kafka│ │ Silo / S3   │
                    │ :3307     │ │:9092│ │ :9100/:9101 │
                    └───────────┘ └─────┘ └─────────────┘
                             │
                         SMTP provider
```

The API stores relational data and audit records in MySQL. Document binaries are stored in Silo, while document metadata, versions, permissions, and audit records remain in MySQL. Audit events are published to the `audit-events` Kafka topic and consumed into the audit log table. Keycloak and SMTP are external dependencies and are not started by `compose.yml`.

## Technology stack

- **Backend:** Java 25, Spring Boot 4.1.1, Spring Web MVC, Spring Security, Spring Data JPA, Spring Kafka, Spring Mail, Thymeleaf, Springdoc OpenAPI
- **Persistence:** MySQL 9.4 for local Compose development, Flyway migrations
- **Messaging:** Apache Kafka in KRaft mode
- **Object storage:** Silo with the AWS SDK for Java S3 client
- **Frontend:** React 19, Vite 6, `keycloak-js`
- **Testing:** JUnit/Spring test support and Testcontainers for MySQL and Kafka
- **CI:** GitHub Actions with Temurin Java 25 and Node.js 22

## Requirements

Install the following before starting local development:

- Java 25
- Docker Engine with Docker Compose
- Node.js 22 and npm
- A Keycloak realm and OIDC client
- An SMTP account for email features

Maven is not required separately when using the included Maven Wrapper (`mvnw`/`mvnw.cmd`).

## Quick start

### 1. Configure local infrastructure

Create a `.env` file in the repository root. Silo requires both credentials:

```dotenv
SILO_ROOT_USER=silo-admin
SILO_ROOT_PASSWORD=replace-with-a-local-password
```

Start MySQL, Kafka, and Silo:

```bash
docker compose up -d
```

Check service status:

```bash
docker compose ps
```

Local service endpoints:

| Service | Address | Purpose |
| --- | --- | --- |
| MySQL | `localhost:3307` | Application database |
| Kafka | `localhost:9092` | Audit event transport |
| Silo API | `http://localhost:9100` | S3-compatible object storage API |
| Silo console | `http://localhost:9101` | Local object storage administration |

Stop the services without deleting data:

```bash
docker compose down
```

Delete the local database, Kafka, and Silo volumes as well:

```bash
docker compose down -v
```

### 2. Configure Keycloak

The backend validates tokens issued by:

```text
http://localhost:8180/realms/demo
```

Use the following local defaults, or override the frontend values with `frontend/.env`:

| Setting | Default |
| --- | --- |
| Keycloak URL | `http://localhost:8180` |
| Realm | `demo` |
| Frontend client ID | `demo-client` |
| Frontend redirect URI | `http://localhost:5173/` |

Configure the client as a browser/public client using the Authorization Code flow with PKCE. Add `http://localhost:5173/*` to valid redirect URIs and `http://localhost:5173` to allowed web origins.

The API uses Keycloak for token identity, but application authorization roles are loaded from MySQL. A user must therefore be provisioned by calling `/api/me` and must have an active local application role (`USER` or `ADMIN`) before protected workflows can be used. `ADMIN` is required for `/api/admin/**`, except audit endpoints, which allow authenticated users and apply additional event-level authorization.

### 3. Configure the backend

The defaults in `src/main/resources/application.yml` are suitable for the local Compose services. Set the following variables when using different infrastructure or real email delivery:

```dotenv
DB_URL=jdbc:mysql://localhost:3307/demo_db
DB_USERNAME=root
DB_PASSWORD=local-db-password

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_AUDIT_GROUP=audit-log-consumer

SILO_ROOT_USER=silo-admin
SILO_ROOT_PASSWORD=replace-with-a-local-password

FRONTEND_ORIGIN=http://localhost:5173

MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-user
MAIL_PASSWORD=your-smtp-password
MAIL_FROM=no-reply@example.com
```

Do not commit `.env` files, access tokens, or real credentials. Use a secret manager for deployed environments.

### 4. Start the API

From the repository root:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080`. Flyway applies migrations from `src/main/resources/db/migration` during startup. Hibernate is configured with `ddl-auto: validate`, so schema changes must be delivered through migrations.

### 5. Start the frontend

In a second terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open <http://localhost:5173>. The Vite development server proxies `/api` requests to `http://127.0.0.1:8080`.

Optional `frontend/.env` values:

```dotenv
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=demo
VITE_KEYCLOAK_CLIENT_ID=demo-client
VITE_KEYCLOAK_REDIRECT_URI=http://localhost:5173/
```

For a deployed API, update the proxy in `frontend/vite.config.js` or configure the hosting layer to route `/api` to the backend. Keep `FRONTEND_ORIGIN` aligned with the actual browser origin.

## API reference

With the backend running:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Public connectivity check: `GET http://localhost:8080/api/public`

Protected requests require:

```http
Authorization: Bearer <keycloak-access-token>
```

### Endpoint overview

| Area | Method and path | Access |
| --- | --- | --- |
| Public check | `GET /api/public` | Public |
| Current identity | `GET /api/me` | Authenticated |
| Documents | `POST /api/documents` | Authenticated |
| Documents | `GET /api/documents` | Authenticated |
| Documents | `GET /api/documents/{id}/download` | Authenticated and authorized |
| Document versions | `GET /api/documents/{id}/versions` | Authenticated and authorized |
| Document versions | `POST /api/documents/{id}/versions` | Authenticated and authorized |
| Documents | `DELETE /api/documents/{id}` | Authenticated and authorized |
| Users | `GET /api/admin/users` | `ADMIN` |
| Roles | `GET /api/admin/roles` | `ADMIN` |
| User roles | `GET /api/admin/users/{userId}/roles` | `ADMIN` |
| Assign role | `POST /api/admin/users/{userId}/roles/{roleName}` | `ADMIN` |
| Remove role | `DELETE /api/admin/users/{userId}/roles/{roleName}` | `ADMIN` |
| Email | `POST /api/admin/emails/send` | `ADMIN` |
| Email templates | `GET/POST /api/admin/email-templates` | `ADMIN` |
| Email template | `GET/PUT/DELETE /api/admin/email-templates/{name}` | `ADMIN` |
| Template preview | `POST /api/admin/email-templates/preview` | `ADMIN` |
| Audit search | `GET /api/admin/audit-logs` | `USER` or `ADMIN` |
| Audit event | `GET /api/admin/audit-logs/{eventId}` | `ADMIN` or event-authorized user |

Audit searches accept `actorId`, `action`, `entityType`, `entityId`, `from`, `to`, `page`, and `size`. The date filters use ISO dates (`YYYY-MM-DD`); pagination is zero-based and defaults to page `0` with a page size of `50`.

### Example requests

Check the API without authentication:

```bash
curl http://localhost:8080/api/public
```

Provision the current user after obtaining a Keycloak token:

```bash
curl \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  http://localhost:8080/api/me
```

Upload a document:

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -F "title=Quarterly report" \
  -F "description=Q4 operational report" \
  -F "file=@./quarterly-report.pdf"
```

Search audit events:

```bash
curl "http://localhost:8080/api/admin/audit-logs?action=DOCUMENT_UPLOADED&page=0&size=25" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

Send a plain-text email:

```bash
curl -X POST http://localhost:8080/api/admin/emails/send \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "to": ["recipient@example.com"],
    "subject": "Test message",
    "body": "This message was sent by Qeema Util."
  }'
```

## Postman

Import the following files:

- Collection: `postman/Qeema-Util.postman_collection.json`
- Environment: `postman/Qeema-Util.postman_environment.json`

Set the environment's `accessToken` variable to a valid Keycloak access token. The collection includes public, identity, role, email, template, document, and audit requests.

## Configuration reference

| Variable | Default | Description |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3307/demo_db` | JDBC URL for MySQL |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | `local-db-password` | MySQL password |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `KAFKA_AUDIT_GROUP` | `audit-log-consumer` | Kafka consumer group for audit events |
| `SILO_ROOT_USER` | `silo-admin` | Silo/S3 access key |
| `SILO_ROOT_PASSWORD` | `silo-password-change-me` | Silo/S3 secret key |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | Allowed browser origin for CORS |
| `MAIL_HOST` | `smtp-relay.brevo.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | empty | SMTP username |
| `MAIL_PASSWORD` | empty | SMTP password |
| `MAIL_FROM` | `no-reply@example.com` | Default sender address |
| `DOCUMENT_MAX_SIZE_BYTES` | `52428800` | Maximum document size, 50 MiB |
| `DOCUMENT_ALLOWED_CONTENT_TYPES` | `application/pdf,text/plain,image/png,image/jpeg` | Comma-separated allowed MIME types |

The Silo endpoint, bucket name, region, and Keycloak issuer are currently defined in `src/main/resources/application.yml`. Change them there or introduce environment-backed configuration before deploying to another environment.

## Development

### Backend

```bash
./mvnw validate
./mvnw test
./mvnw verify
```

The Maven wrapper is executable on Unix-like systems. If needed:

```bash
chmod +x mvnw
```

### Frontend

```bash
cd frontend
npm ci
npm run build
npm run preview
```

The frontend currently exposes `dev`, `build`, and `preview` scripts; linting is not configured as a separate npm script.

### Continuous integration

`.github/workflows/ci.yml` runs on pushes and pull requests targeting `main` or `develop`. It:

1. Validates and verifies the backend with Java 25.
2. Builds the frontend with Node.js 22.
3. Uploads backend reports, the backend JAR, and the frontend bundle as artifacts.
4. Runs dependency review for pull requests and fails on high-severity dependency changes.

## Project layout

```text
.
├── src/main/java/com/qeema/util/
│   ├── audit/          Audit events, Kafka processing, queries, and APIs
│   ├── document/       Document metadata, versions, permissions, and storage
│   ├── identity/       User provisioning and application role administration
│   ├── notification/   Email delivery, templates, and delivery logs
│   ├── security/       JWT conversion, authorization, and CORS
│   └── storage/        S3-compatible Silo integration
├── src/main/resources/
│   ├── db/migration/   Versioned Flyway migrations
│   └── templates/email/ Built-in Thymeleaf email templates
├── src/test/           Backend tests and Testcontainers configuration
├── frontend/src/       React administration console and Keycloak client
├── postman/            API collection and local environment
├── compose.yml         Local MySQL, Kafka, and Silo services
└── .github/workflows/  CI and CodeQL workflows
```

## Operations and troubleshooting

### Check local containers

```bash
docker compose ps
docker compose logs -f mysql kafka silo
```

### Database migration failures

Confirm that MySQL is ready and that `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` match the running container. Do not edit an applied migration; add a new versioned migration instead.

### Authentication failures

Verify that:

1. Keycloak is reachable at the issuer URL configured by the backend.
2. The token is unexpired and was issued for the expected realm.
3. The token contains the identity claims required by the application.
4. The user exists locally, is active, and has an application role in MySQL.
5. `FRONTEND_ORIGIN` matches the browser origin exactly.

### Document upload failures

Check that Silo is running, its credentials match the API configuration, and the file MIME type and size comply with `DOCUMENT_ALLOWED_CONTENT_TYPES` and `DOCUMENT_MAX_SIZE_BYTES`. The `util-bucket` bucket is created on first use if it does not already exist.

### Kafka audit failures

Check that Kafka is listening on `localhost:9092` (or the configured bootstrap server). The producer publishes to `audit-events`; the consumer group defaults to `audit-log-consumer`.

## Security and production guidance

- Replace all development credentials and default passwords.
- Keep Keycloak, MySQL, Kafka, Silo, and SMTP on private networks where possible.
- Use HTTPS for the frontend, API, Keycloak, and storage administration endpoints.
- Restrict `FRONTEND_ORIGIN` to the deployed frontend origin; do not use a wildcard CORS policy.
- Store secrets in a managed secret store, not in source control or committed environment files.
- Review document MIME and size limits for the deployment's threat model.
- Monitor API, Kafka consumer, database, and storage logs.
- Back up MySQL and Silo data independently.
- Treat Flyway migrations as immutable after release.

## Contributing

1. Create a focused branch from `develop`.
2. Make the smallest complete change that addresses the issue.
3. Update migrations, API documentation, configuration notes, or this README when behavior changes.
4. Run `./mvnw verify` and `npm run build` before opening a pull request.
5. Include deployment and rollback notes for schema or infrastructure changes.

## License

No license has been declared for this repository. Contact the project maintainers before redistributing the software.
