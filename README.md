# Qeema Util

Qeema Util is a secure operations platform for document management, identity and role administration, transactional email, and searchable audit history.

The repository contains:

- A Spring Boot API secured as an OAuth2 resource server with Keycloak-issued JWTs.
- A React/Vite administration console.
- MySQL persistence with Flyway-managed migrations.
- Kafka-backed audit event processing.
- S3-compatible object storage through Silo.
- OpenAPI documentation and a ready-to-import Postman collection.

## Features

- **Documents** — upload, list, download, soft-delete, and create new document versions.
- **Identity and roles** — provision the authenticated user and manage application roles for users.
- **Email center** — create, update, preview, and delete templates, then send plain-text or template-based emails.
- **Audit trail** — search and inspect audit events by actor, action, entity, and date range.
- **Protected API** — public health access, authenticated user access, and role-based admin/user authorization.
- **Admin console** — browser UI for the document, email, audit, and identity workflows.

## Architecture

```text
React/Vite frontend (:5173)
          │
          │ Bearer JWT
          ▼
Spring Boot API (:8080) ───── Keycloak (:8180, external)
          │
          ├── MySQL (:3307 by default, external)
          ├── Kafka (:9092, Docker Compose)
          ├── Silo / S3-compatible storage (:9100, Docker Compose)
          └── SMTP provider (external)
```

Database migrations are applied automatically by Flyway when the API starts. Audit events are published to Kafka and consumed into the audit log store. Uploaded document content is kept in Silo; document metadata, versions, permissions, and audit records are stored in MySQL.

## Requirements

- Java 25
- Maven 3.9+ (or use the included Maven Wrapper)
- Node.js 22 and npm
- Docker Engine with Docker Compose
- MySQL 8+
- A Keycloak realm and client configured to issue JWTs
- An SMTP account for email features

The CI workflow uses Temurin Java 25 and Node.js 22.

## Quick start

### 1. Start local infrastructure

Create a `.env` file in the repository root. At minimum, define credentials for Silo:

```dotenv
SILO_ROOT_USER=silo-admin
SILO_ROOT_PASSWORD=change-this-password
```

Start Kafka and Silo:

```bash
docker compose up -d
```

The Compose services are available at:

| Service | URL |
| --- | --- |
| Kafka | `localhost:9092` |
| Silo API | `http://localhost:9100` |
| Silo console | `http://localhost:9101` |

The API creates the configured Silo bucket (`util-bucket`) on the first document upload if it does not already exist.

### 2. Configure MySQL, Keycloak, and SMTP

Create a MySQL database and user, then configure the API with environment variables. The default local database URL is `jdbc:mysql://localhost:3307/demo_db`.

Keycloak must expose the issuer configured by the application:

```text
http://localhost:8180/realms/demo
```

The frontend defaults to the Keycloak realm `demo` and client `demo-client`. Configure that client for the frontend origin (`http://localhost:5173`) and enable the standard authorization code flow with PKCE.

Example API environment:

```dotenv
DB_URL=jdbc:mysql://localhost:3307/demo_db
DB_USERNAME=root
DB_PASSWORD=local-db-password

MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-user
MAIL_PASSWORD=your-smtp-password

FRONTEND_ORIGIN=http://localhost:5173
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SILO_ROOT_USER=silo-admin
SILO_ROOT_PASSWORD=change-this-password
```

Do not commit `.env` files or real credentials. The repository ignores `.env` by default.

### 3. Run the backend

From the repository root:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts on `http://localhost:8080`. Flyway runs the SQL files in `src/main/resources/db/migration` during startup.

### 4. Run the frontend

In a second terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. Optional Vite variables can be supplied through `frontend/.env`:

```dotenv
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=demo
VITE_KEYCLOAK_CLIENT_ID=demo-client
VITE_KEYCLOAK_REDIRECT_URI=http://localhost:5173/
```

The Vite development server proxies `/api` requests to `http://localhost:8080` by default. If the API is hosted elsewhere, update the proxy in `frontend/vite.config.js` and keep `FRONTEND_ORIGIN` aligned with the frontend origin.

## API documentation

With the backend running, use:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Public check: `GET http://localhost:8080/api/public`

Protected endpoints require an `Authorization: Bearer <access-token>` header. `/api/admin/**` requires the `ADMIN` role. User-facing protected endpoints accept `USER` or `ADMIN` where configured by the security layer.

### Main endpoints

| Area | Endpoints |
| --- | --- |
| Identity | `GET /api/public`, `GET /api/me` |
| Documents | `POST/GET /api/documents`, `GET /api/documents/{id}/download`, `GET/POST /api/documents/{id}/versions`, `DELETE /api/documents/{id}` |
| Roles | `GET /api/admin/roles`, `GET /api/admin/users/{userId}/roles`, `POST/DELETE /api/admin/users/{userId}/roles/{roleName}` |
| Email | `POST /api/admin/emails/send` |
| Email templates | `GET/POST /api/admin/email-templates`, `GET/PUT/DELETE /api/admin/email-templates/{name}`, `POST /api/admin/email-templates/preview` |
| Audit | `GET /api/admin/audit-logs`, `GET /api/admin/audit-logs/{eventId}` |

Audit searches support `actorId`, `action`, `entityType`, `entityId`, `from`, `to`, `page`, and `size` query parameters.

## Postman

Import these files into Postman:

- Collection: `postman/Qeema-Util.postman_collection.json`
- Environment: `postman/Qeema-Util.postman_environment.json`

Set the environment's `accessToken` to a Keycloak access token. The collection includes public, identity, role, email, template, document, and audit requests.

## Development

### Backend checks

```bash
./mvnw validate
./mvnw verify
```

### Frontend checks

```bash
cd frontend
npm ci
npm run build
```

The GitHub Actions workflow runs backend validation and verification, builds the frontend, stores build/test artifacts, and performs dependency review for pull requests.

### Project layout

```text
src/main/java/com/qeema/util/
├── audit/          Kafka publishing, consumption, querying, and audit APIs
├── document/       Document metadata, versions, permissions, and storage
├── identity/       User provisioning and application role management
├── notification/   Email sending, templates, and delivery logs
├── security/       JWT role conversion, authorization, and CORS
└── storage/        S3-compatible Silo client configuration

src/main/resources/
├── db/migration/   Flyway migrations
└── templates/email/ Built-in Thymeleaf email templates

frontend/src/       React admin console and Keycloak client
postman/            API collection and local environment
```

## Configuration reference

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3307/demo_db` | JDBC connection URL |
| `DB_USERNAME` | `root` | Database user |
| `DB_PASSWORD` | `local-db-password` | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `KAFKA_AUDIT_GROUP` | `audit-log-consumer` | Audit consumer group |
| `MAIL_HOST` | — | SMTP host; required |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | — | SMTP username; required |
| `MAIL_PASSWORD` | — | SMTP password; required |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | Allowed browser origin |
| `SILO_ROOT_USER` | `silo-admin` | Silo/S3 access key |
| `SILO_ROOT_PASSWORD` | `silo-password-change-me` | Silo/S3 secret key |

For production, replace all development defaults, use managed infrastructure where appropriate, restrict CORS to the deployed frontend origin, and keep secrets in a secret manager rather than environment files committed to source control.

## Contributing

1. Create a focused branch from `develop`.
2. Make the smallest change that fully addresses the problem.
3. Run `./mvnw verify` and `npm run build` before opening a pull request.
4. Update API documentation, migrations, or this README when behavior or setup changes.
5. Include migration and configuration notes for changes that affect deployment.

## License

No license has been declared for this repository yet. Contact the project maintainers before redistributing the software.
