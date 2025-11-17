# Soma Server Project Structure

```
soma_server/
├── README.md
├── database/
│   └── init.sql
├── docker/
│   ├── docker-compose.yml
│   ├── Dockerfile
│   └── init/
├── postman/
│   ├── soma_server.postman_collection.json
│   └── soma_server_local.postman_environment.json
├── server/
│   ├── .gitattributes
│   ├── .gitignore
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   ├── .mvn/
│   │   └── wrapper/
│   │       └── maven-wrapper.properties
│   ├── .vscode/
│   │   └── NEWLY_CREATED_BY_SPRING_INITIALIZR
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── soma/
│   │   │   │           └── server/
│   │   │   │               ├── ServerApplication.java         # Main Spring Boot app
│   │   │   │               ├── config/
│   │   │   │               │   ├── SecurityConfig.java        # Security & Password encoder
│   │   │   │               │   └── SpotifyConfig.java         # Spotify OAuth config
│   │   │   │               ├── controller/
│   │   │   │               │   ├── MainController.java        # Main page controller
│   │   │   │               │   ├── RegisterController.java    # POST /api/register
│   │   │   │               │   ├── SpotifyController.java     # Spotify OAuth endpoints
│   │   │   │               │   └── UsersController.java       # User CRUD endpoints
│   │   │   │               ├── entity/
│   │   │   │               │   ├── SpotifyUserDetails.java    # Spotify user data
│   │   │   │               │   ├── User.java                  # Main User JPA entity
│   │   │   │               │   └── YandexUserDetails.java     # Yandex user data
│   │   │   │               ├── repository/
│   │   │   │               │   └── UserRepository.java        # JPA repository for User
│   │   │   │               └── service/
│   │   │   │                   ├── RegistrationService.java   # User registration logic
│   │   │   │                   ├── UserDetailsService.java    # Spring Security integration
│   │   │   │                   └── UserService.java          # Spotify user management
│   │   │   └── resources/
│   │   │       ├── application-docker.properties             # Docker profile config
│   │   │       ├── application.properties                    # Local dev config
│   │   │       ├── static/                                   # Static web assets
│   │   │       └── templates/                                # Thymeleaf templates
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── soma/
│   │                   └── server/
│   │                       └── ServerApplicationTests.java   # Basic test
│   └── target/                                               # Build output
│       ├── server-0.0.1-SNAPSHOT.jar                        # Executable JAR
│       ├── server-0.0.1-SNAPSHOT.jar.original
│       ├── classes/                                          # Compiled classes
│       ├── generated-sources/
│       ├── generated-test-sources/
│       ├── maven-archiver/
│       ├── maven-status/
│       └── test-classes/
└── sql/
    └── SQLQuerySoma.sql

## Key Components:

### 🔧 Configuration
- **SecurityConfig.java**: BCrypt password encoder, permits all endpoints
- **SpotifyConfig.java**: Spotify OAuth2 client configuration

### 🌐 API Endpoints
- **POST** `/soma/api/register` - User registration
- **GET** `/soma/api/users/exists?email=...` - Check user existence
- **GET** `/soma/api/users/by-email/{email}` - Get user by email
- **GET** `/soma/api/users/by-username/{username}` - Get user by username
- **GET** `/soma/api/users/by-id/{id}` - Get user by ID

### 💾 Data Layer
- **User.java**: JPA entity with username, email, hashed password
- **UserRepository.java**: Spring Data JPA repository
- **RegistrationService.java**: Business logic for user registration

### 🐳 Docker Setup
- **Dockerfile**: Multi-stage build for Spring Boot app
- **docker-compose.yml**: PostgreSQL + Spring Boot app
- **Database**: PostgreSQL 15 on port 5433 (local) / 5432 (container)

### 📮 Testing
- **Postman Collection**: Ready-to-use API tests
- **Local Environment**: Port 8081 (local dev)
- **Docker Environment**: Port 8080 (containerized)
```

## Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Postman/UI    │    │  Spring Boot    │    │   PostgreSQL    │
│                 │    │     App         │    │    Database     │
│ POST /register  │───▶│                 │───▶│     users       │
│ GET /users/*    │    │ Port 8080/8081  │    │   table         │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   Spotify/      │
                    │   Yandex OAuth  │
                    │   (Future)      │
                    └─────────────────┘
```