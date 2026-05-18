# PROYECTO-KINETICA

Backend MVP para aplicacion de traduccion de lenguaje de señas peruano (LSP) a texto/audio en Espanol, y viceversa.

## Stack Tecnologico

- Java 17
- Spring Boot 4.0.6
- Spring Data JPA
- Spring Security (JWT + OAuth2)
- PostgreSQL 15
- Maven

## Modulos

- **auth**: Autenticacion JWT, refresh tokens, roles (USER/ADMIN)
- **sign**: Catalogo de señas (CRUD)
- **translation**: Traducciones async con patron outbox
- **media**: Referencias a contenido multimedia
- **feedback**: Feedback de usuarios

## Configuracion

Las variables de entorno se configuran en `.env`:

```bash
POSTGRES_PORT=5432
POSTGRES_DB=kinetica
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=tu_password
APP_SECURITY_JWT_SECRET=tu_secret_jwt_min_32_chars

# Email (opcional)
APP_MAIL_ENABLED=false
APP_MAIL_FROM=no-reply@kinetica.local
SPRING_MAIL_HOST=
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=

# OAuth2 Google (opcional)
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE=openid,profile,email
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI={baseUrl}/login/oauth2/code/{registrationId}
APP_OAUTH2_SUCCESS_REDIRECT=http://localhost:3000/auth/callback
APP_OAUTH2_FAILURE_REDIRECT=http://localhost:3000/auth/error
```

## Ejecucion Local

```bash
# Cargar variables de entorno
source .env

# Correr aplicacion
./mvnw spring-boot:run

# O usar el script
./run-spring.sh
```

## Tests

```bash
./mvnw test
```

## Construccion

```bash
./mvnw package
```

## CI/CD

GitHub Actions corre los tests automaticamente en pushes a main/develop y PRs.

## Endpoints Principales

- `POST /auth/register` - Registro de usuarios
- `POST /auth/login` - Login
- `GET /oauth2/authorization/google` - Iniciar login con Google (si está configurado)
- `POST /auth/refresh` - Refresh token
- `POST /auth/logout` - Logout
- `GET /signs` - Listar señas (requiere auth)
- `POST /signs` - Crear señas (solo ADMIN)
- `POST /translations` - Crear traduccion
- `GET /translations` - Listar traducciones
- `POST /translations/{requestId}/media/upload` - Subir media gestionada por backend
- `GET /kpis/translations?days=7` - KPIs de traduccion (solo ADMIN)
- `POST /linguistics/es-to-gloss` - Convertir español natural a glosa LSP
- `POST /linguistics/gloss-to-es` - Convertir glosa LSP a español natural
- `GET /users` - Listar usuarios (solo ADMIN)
- `GET /roles` - Listar roles (solo ADMIN)

## Glosa como Intermediario (Pipeline)

- **TEXT_TO_SIGN**: Español natural -> **Glosa LSP** -> IA de señas
- **SIGN_TO_TEXT**: IA (glosa/candidata) -> **Glosa LSP** -> Español natural

El backend aplica esta lógica en `TranslationRequestedEventHandler` y expone `glossOutput` en `TranslationResponse`.

### System prompts activos (GitHub Models)

Los prompts de ambos sentidos están implementados en:

- `GithubModelsGlossClient.SYSTEM_ES_TO_GLOSS`
- `GithubModelsGlossClient.SYSTEM_GLOSS_TO_ES`

Ambos fuerzan salida JSON estructurada para reducir variabilidad del modelo.

## OAuth2 Google: callback contract

- El backend inicia el flujo en `GET /oauth2/authorization/google`.
- En login exitoso, redirige a `APP_OAUTH2_SUCCESS_REDIRECT?oauth=success`.
- Los tokens internos (access + refresh) y metadatos de sesión se envían como cookies `HttpOnly`:
  - `kinetica_access_token`
  - `kinetica_refresh_token`
  - `kinetica_token_type`
  - `kinetica_user_id`
  - `kinetica_user_email`
- En login fallido, redirige a `APP_OAUTH2_FAILURE_REDIRECT?error=oauth_login_failed` y limpia las cookies anteriores.

## Diagrama Entidad Relacion

```mermaid
erDiagram
    USER ||--o{ USER_ROLE : has
    ROLE ||--o{ USER_ROLE : assigned_to
    USER ||--o{ TRANSLATION_REQUEST : creates
    TRANSLATION_REQUEST ||--o| TRANSLATION_RESULT : produces
    TRANSLATION_REQUEST ||--o{ OUTBOX_EVENT : triggers

    USER {
        bigint id PK
        string email
        string passwordHash
    }

    ROLE {
        bigint id PK
        string name
    }

    USER_ROLE {
        bigint id PK
        bigint user_id FK
        bigint role_id FK
    }

    SIGN {
        bigint id PK
        string label
        string normalizedLabel
        string mediaRef
        string locale
        boolean active
    }

    TRANSLATION_REQUEST {
        bigint id PK
        bigint userId FK
        string direction
        string status
        text sourceText
    }

    TRANSLATION_RESULT {
        bigint id PK
        bigint request_id FK
        text textOutput
        text glossOutput
        string signOutputRef
        double confidence
        bigint latencyMs
        string modelVersion
        string warning
    }

    OUTBOX_EVENT {
        bigint id PK
        string eventType
        string status
        int retryCount
        int maxRetries
        timestamp nextRetryAt
    }
```

## Descripcion de Entidades

- **User**: Usuario del sistema (id, email, passwordHash)
- **Role**: Rol (id, name) -> USER, ADMIN
- **UserRole**: Relacion usuario-rol
- **Sign**: Seña del catalogo (id, label, normalizedLabel, mediaRef, locale, active)
- **TranslationRequest**: Solicitud de traduccion (id, userId, direction, status, sourceText)
- **TranslationResult**: Resultado (id, request_id, textOutput, glossOutput, signOutputRef, confidence, latencyMs, modelVersion, warning)
- **OutboxEvent**: Evento outbox para async (id, status, retryCount, maxRetries, nextRetryAt)

## Relaciones

- User 1:N UserRole N:1 Role
- User 1:N TranslationRequest
- TranslationRequest 1:1 TranslationResult
- TranslationRequest 1:N OutboxEvent

## Roles

- `USER` - Usuario regular
- `ADMIN` - Administrador

## Arquitectura de Seguridad

- JWT stateless con session STATELESS
- Deny-by-default: todas las rutas bloqueadas salvo allowlist
- Validacion de issuer/audience en JWT
- Refresh tokens con rotacion y lock pesimista
- Outbox con reintentos y backoff exponencial
