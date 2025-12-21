# Soma Server (Backend)

The backend for Soma, built with Spring Boot. It handles Spotify OAuth, playlist processing, statistics aggregation, and user management.

## Features

- **Spotify Integration**: OAuth2 authentication and API wrapper.
- **Yandex Music Parsing**: Scrapes public playlist data from Yandex Music.
- **Statistics Engine**: Aggregates user data (top artists, genres).
- **Async Transfer**: Background processing for large playlist transfers with progress tracking.
- **AI Chat Integration**: OpenRouter API integration for the chat assistant.
- **Database**: PostgreSQL for storing users, history, and chat sessions.

## Tech Stack

- **Java 21**: Core language
- **Spring Boot 3**: Framework
- **Spring Data JPA**: Database access
- **PostgreSQL**: Database
- **Lombok**: Boilerplate reduction
- **Swagger/OpenAPI**: API Documentation

## Getting Started

1. **Prerequisites**
   - Java 21 SDK
   - Docker & Docker Compose (for DB)

2. **Run Database**
   ```bash
   cd docker
   docker-compose up -d
   ```

3. **Run Application**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Run Tests**
   ```bash
   ./mvnw test
   ```

## Configuration

- `application.properties`: Main configuration (DB, Spotify API keys, etc.).
