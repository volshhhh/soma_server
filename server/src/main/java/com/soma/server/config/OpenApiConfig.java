package com.soma.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI somaOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .tags(apiTags())
                .components(securityComponents())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private Info apiInfo() {
        return new Info()
                .title("SOMA API")
                .description("""
                    # SOMA — Платформа для трансфера музыки и аналитики
                    
                    SOMA — это современная веб-платформа для переноса музыкальных плейлистов 
                    между стриминговыми сервисами и глубокой аналитики музыкальных предпочтений.
                    
                    ## Основные возможности
                    
                    - 🔄 **Перенос плейлистов** — Yandex Music → Spotify (создание новых или добавление в существующие)
                    - 📊 **Музыкальная статистика** — топ артистов, треков, жанров за разные периоды
                    - 📜 **История переносов** — полный лог всех операций с прогрессом
                    - 🤖 **AI-ассистент** — интеллектуальная помощь по навигации
                    - 👤 **Управление профилем** — редактирование данных, подключение сервисов
                    
                    ## Аутентификация
                    
                    Система поддерживает два метода аутентификации:
                    
                    ### 1. Email/Password + JWT
                    - Регистрация: `POST /soma/api/auth/register`
                    - Вход: `POST /soma/api/auth/login`
                    - Обновление токена: `POST /soma/api/auth/refresh`
                    - Токены: Access (24ч) + Refresh (7 дней)
                    
                    ### 2. Spotify OAuth 2.0
                    - Получить URL: `GET /soma/api/login`
                    - Callback: `GET /soma/api/callback`
                    - Подключение к существующему аккаунту: `GET /soma/api/connect-spotify`
                    
                    ## Авторизация запросов
                    
                    Для защищённых эндпоинтов добавляйте заголовок:
                    ```
                    Authorization: Bearer <access_token>
                    ```
                    
                    ## Коды ответов
                    
                    | Код | Описание |
                    |-----|----------|
                    | 200 | Успешный запрос |
                    | 201 | Ресурс создан |
                    | 400 | Некорректный запрос |
                    | 401 | Не авторизован |
                    | 403 | Доступ запрещён |
                    | 404 | Ресурс не найден |
                    | 500 | Ошибка сервера |
                    """)
                .version("2.0.0")
                .contact(new Contact()
                        .name("SOMA Team")
                        .email("support@soma-app.com")
                        .url("https://github.com/soma-app"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> apiServers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local Development Server"),
                new Server()
                        .url("http://localhost:8080")
                        .description("Docker Development Server")
        );
    }

    private List<Tag> apiTags() {
        return List.of(
                new Tag()
                        .name("Authentication")
                        .description("""
                            Эндпоинты аутентификации пользователей.
                            
                            - **JWT Auth**: Регистрация и вход через email/password
                            - **OAuth2**: Вход через Spotify
                            - **Token Management**: Обновление и валидация токенов
                            """),
                new Tag()
                        .name("Users")
                        .description("Управление пользователями и регистрация (legacy)"),
                new Tag()
                        .name("Spotify")
                        .description("""
                            Интеграция со Spotify API.
                            
                            - **OAuth Flow**: Авторизация через Spotify
                            - **Playlist Transfer**: Перенос плейлистов из Yandex Music
                            - **Connect Account**: Подключение Spotify к существующему аккаунту
                            """),
                new Tag()
                        .name("Profile")
                        .description("""
                            Управление профилем пользователя.
                            
                            - **View/Edit**: Просмотр и редактирование данных
                            - **Connected Services**: Управление подключёнными сервисами
                            - **Subscription**: Управление подпиской
                            """),
                new Tag()
                        .name("Statistics")
                        .description("""
                            Музыкальная статистика и аналитика.
                            
                            - **Top Artists**: Топ исполнителей за разные периоды
                            - **Top Tracks**: Топ треков
                            - **Genres**: Анализ жанровых предпочтений
                            - **Transfer History**: История переносов плейлистов
                            """),
                new Tag()
                        .name("Search")
                        .description("""
                            Поиск по каталогу Spotify.
                            
                            - **Tracks**: Поиск треков
                            - **Artists**: Поиск артистов
                            - **Albums**: Поиск альбомов
                            - **Playlists**: Поиск плейлистов
                            """),
                new Tag()
                        .name("Chat")
                        .description("""
                            AI-ассистент на базе OpenRouter.
                            
                            - **Sessions**: Управление сессиями чата
                            - **Messages**: Отправка и получение сообщений
                            - **Context-aware**: Ассистент понимает контекст пользователя
                            """)
        );
    }

    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                    JWT Access Token для авторизации запросов.
                                    
                                    **Получение токена:**
                                    1. Email/Password: `POST /soma/api/auth/login`
                                    2. Spotify OAuth: `GET /soma/api/callback` (автоматически)
                                    
                                    **Время жизни:**
                                    - Access Token: 24 часа
                                    - Refresh Token: 7 дней
                                    
                                    **Обновление:** `POST /soma/api/auth/refresh`
                                    """));
    }
}
