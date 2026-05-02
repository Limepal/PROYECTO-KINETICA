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
- `POST /auth/refresh` - Refresh token
- `POST /auth/logout` - Logout
- `GET /signs` - Listar señas (requiere auth)
- `POST /signs` - Crear señas (solo ADMIN)
- `POST /translations` - Crear traduccion
- `GET /translations` - Listar traducciones
- `GET /users` - Listar usuarios (solo ADMIN)
- `GET /roles` - Listar roles (solo ADMIN)

## Roles

- `USER` - Usuario regular
- `ADMIN` - Administrador

## Arquitectura de Seguridad

- JWT stateless con session STATELESS
- Deny-by-default: todas las rutas bloqueadas salvo allowlist
- Validacion de issuer/audience en JWT
- Refresh tokens con rotacion y lock pesimista
- Outbox con reintentos y backoff exponencial