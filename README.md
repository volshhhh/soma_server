# Soma - Backend Service

## 🎵 О проекте

Soma - это универсальный сервис для переноса музыкальных плейлистов и предпочтений между популярными стриминговыми платформами с расширенной аналитикой прослушиваний.

**Основные возможности:**
- 🔄 Перенос плейлистов между Spotify, YouTube Music, Yandex Music и другими платформами
- 📊 Агрегация статистики прослушиваний со всех подключенных сервисов
- 🔐 Безопасное хранение и обработка пользовательских данных

## 🛠 Технологический стек

**Backend:**
- Java 21
- Spring Boot 3.5
- Spring Security с OAuth 2.0
- PostgreSQL
- Redis для кэширования
- Maven

**Интеграции:**
- Spotify Web API
- YouTube Data API
- Yandex Music API
- Apple Music API (в разработке)

**Инфраструктура:**
- Docker & Docker Compose
- Nginx
- AWS/Google Cloud
- GitHub Actions CI/CD

## 📋 Требования

- Java 21 или выше
- PostgreSQL 14+
- Redis 6+
- Maven 3.6+

## 🚀 Быстрый старт

### Локальная разработка

1. **Клонирование репозитория**
```bash
git clone https://github.com/volshhhh/soma_backend.git
cd soma-backend
```

2. **Настройка базы данных**
```bash
# Запуск PostgreSQL и Redis через Docker
docker-compose up -d postgres redis
```

3. **Настройка переменных окружения**
```bash
cp .env.example .env
# Отредактируйте .env файл с вашими настройками
```

4. **Запуск приложения**
```bash
mvn spring-boot:run
```

Приложение будет доступно по адресу: `http://localhost:8080`

### Docker deployment

```bash
# Сборка и запуск всего стека
docker-compose up -d

# Только определенные сервисы
docker-compose up -d app postgres redis
```

## 🔧 Конфигурация

Основные настройки в `application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/soma
    username: soma_user
    password: ${DB_PASSWORD}
  
  security:
    oauth2:
      client:
        registration:
          spotify:
            client-id: ${SPOTIFY_CLIENT_ID}
            client-secret: ${SPOTIFY_CLIENT_SECRET}
```

## 🔐 API Endpoints

### Аутентификация
- `POST /api/auth/login/{provider}` - OAuth аутентификация
- `POST /api/auth/logout` - Выход из системы
- `GET /api/auth/me` - Информация о текущем пользователе

### Плейлисты
- `GET /api/playlists` - Список плейлистов пользователя
- `POST /api/playlists/transfer` - Перенос плейлиста между платформами
- `GET /api/playlists/{id}/status` - Статус переноса

### Статистика
- `GET /api/stats/listening-history` - История прослушиваний
- `GET /api/stats/top-artists` - Топ артистов
- `GET /api/stats/top-tracks` - Топ треков
- `GET /api/stats/genres` - Статистика по жанрам

## 🧪 Тестирование

```bash
# Запуск всех тестов
mvn test

# Запуск с генерацией отчета покрытия
mvn jacoco:report

# Интеграционные тесты
mvn verify -P integration-test
```

## 🤝 Разработка
