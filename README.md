# Soma Chat Server

Серверный модуль чата для приложения Soma (трансфер музыки в Spotify).

## 📋 Описание

Полнофункциональный чат на Spring Boot с поддержкой WebSocket/STOMP протокола и персистентным хранением сообщений в базе данных через JPA.

### Основные возможности

- ✅ Реал-тайм обмен сообщениями через WebSocket
- ✅ STOMP протокол с SockJS fallback
- ✅ Поддержка множественных комнат (rooms)
- ✅ Персистентное хранение сообщений (PostgreSQL / H2)
- ✅ REST API для получения истории сообщений
- ✅ Минимальная аутентификация (легко расширяется)
- ✅ Отслеживание онлайн/офлайн статуса пользователей

## 🛠 Технологический стек

- **Java 17+**
- **Spring Boot 3.2.x**
  - Spring Web
  - Spring WebSocket
  - Spring Data JPA
  - Spring Security
- **База данных**
  - PostgreSQL (продакшен)
  - H2 (разработка)
- **Lombok** - сокращение boilerplate кода

## 📁 Структура проекта

```
src/main/java/com/soma/chat/
├── SomaChatApplication.java       # Точка входа
├── config/
│   ├── WebSocketConfig.java       # Конфигурация STOMP
│   ├── SecurityConfig.java        # Spring Security
│   └── WebSocketEventListener.java # Обработка connect/disconnect
├── controller/
│   ├── ChatWebSocketController.java # STOMP контроллер
│   └── ChatRestController.java      # REST API
├── dto/
│   ├── ChatMessageRequest.java    # Входящее сообщение
│   ├── ChatMessageResponse.java   # Исходящее сообщение
│   ├── ChatEventDto.java          # События чата
│   └── UserDto.java               # Информация о пользователе
├── model/entity/
│   ├── User.java                  # Сущность пользователя
│   └── ChatMessage.java           # Сущность сообщения
├── repository/
│   ├── UserRepository.java        # JPA репозиторий пользователей
│   └── ChatMessageRepository.java # JPA репозиторий сообщений
└── service/
    ├── UserService.java           # Бизнес-логика пользователей
    └── ChatService.java           # Бизнес-логика чата
```

## 🚀 Запуск

### Режим разработки (H2 in-memory)

```bash
# С Maven
mvn spring-boot:run

# Или с Maven Wrapper
./mvnw spring-boot:run
```

Приложение будет доступно на `http://localhost:8080`

### Режим продакшен (PostgreSQL)

1. Создайте базу данных PostgreSQL:

```sql
CREATE DATABASE soma_chat;
CREATE USER soma_user WITH PASSWORD 'soma_password';
GRANT ALL PRIVILEGES ON DATABASE soma_chat TO soma_user;
```

2. Запустите с профилем `prod`:

```bash
mvn spring-boot:run -Dspring.profiles.active=prod

# Или с переменными окружения
DB_USERNAME=your_user DB_PASSWORD=your_password mvn spring-boot:run -Dspring.profiles.active=prod
```

## 🔌 WebSocket API

### Подключение

```javascript
// С SockJS (рекомендуется)
const socket = new SockJS('http://localhost:8080/ws-chat');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Подключено: ' + frame);
});

// Чистый WebSocket
const socket = new WebSocket('ws://localhost:8080/ws-chat');
```

### Подписки (Subscribe)

| Топик | Описание |
|-------|----------|
| `/topic/public` | Публичный канал |
| `/topic/room.{roomId}` | Конкретная комната |
| `/user/queue/history` | Персональная очередь для истории |

### Отправка сообщений (Send)

| Destination | Описание | Payload |
|-------------|----------|---------|
| `/app/chat.send` | Отправка в публичный чат | `{ "roomId": "public", "content": "Hello!" }` |
| `/app/chat.send.{roomId}` | Отправка в комнату | `{ "content": "Hello!" }` |
| `/app/chat.join` | Присоединение к чату | `{ "roomId": "public" }` |
| `/app/chat.history` | Запрос истории | `{ "roomId": "public" }` |

### Формат сообщения (Request)

```json
{
    "roomId": "public",
    "content": "Привет, мир!",
    "type": "TEXT"
}
```

### Формат ответа (Response)

```json
{
    "id": 1,
    "roomId": "public",
    "sender": {
        "id": 1,
        "username": "alice",
        "displayName": "Алиса"
    },
    "content": "Привет, мир!",
    "type": "TEXT",
    "createdAt": "2024-01-15T10:30:00"
}
```

## 🌐 REST API

### История сообщений

```
GET /api/chat/{roomId}/messages?limit=50
```

### История с пагинацией

```
GET /api/chat/{roomId}/messages/paged?page=0&size=20
```

### Поиск по сообщениям

```
GET /api/chat/{roomId}/search?q=текст&page=0&size=20
```

### Количество сообщений

```
GET /api/chat/{roomId}/count
```

### Онлайн пользователи

```
GET /api/chat/users/online
```

### Все пользователи

```
GET /api/chat/users
```

## 👤 Тестовые пользователи

| Username | Password | Роли |
|----------|----------|------|
| alice | password | USER |
| bob | password | USER |
| admin | admin | USER, ADMIN |

## 🔒 Безопасность

Текущая реализация использует in-memory пользователей для демонстрации. 

Для продакшена рекомендуется:

1. **JWT аутентификация** — токен передается при подключении к WebSocket
2. **OAuth2** — интеграция с внешними провайдерами (Google, Spotify)
3. **Сессии с БД** — хранение сессий в Redis/PostgreSQL

### Пример интеграции JWT

```java
// В WebSocketConfig добавьте:
@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = 
                StompHeaderAccessor.wrap(message);
            
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String token = accessor.getFirstNativeHeader("Authorization");
                // Валидация JWT и установка Principal
            }
            return message;
        }
    });
}
```

## 📝 Примечания

### Транзакционность

Все операции сохранения сообщений выполняются в транзакции (`@Transactional`). 
Это гарантирует, что сообщение будет сохранено в БД до отправки подписчикам.

### Масштабирование

Для горизонтального масштабирования:

1. Замените `enableSimpleBroker` на внешний брокер (RabbitMQ, ActiveMQ)
2. Настройте `enableStompBrokerRelay` в `WebSocketConfig`

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableStompBrokerRelay("/topic", "/queue")
        .setRelayHost("localhost")
        .setRelayPort(61613);
}
```

## 📄 Лицензия

MIT License

---

**Soma Chat** — часть проекта по трансферу музыки в Spotify 🎵