package com.soma.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс приложения Soma Chat.
 * 
 * Запускает Spring Boot приложение с модулем чата.
 * Автоматически сканирует и настраивает все компоненты
 * в пакете com.soma.chat и его подпакетах.
 * 
 * Для запуска:
 * - Разработка (H2): mvn spring-boot:run
 * - Продакшен (PostgreSQL): mvn spring-boot:run -Dspring.profiles.active=prod
 */
@SpringBootApplication
public class SomaChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(SomaChatApplication.class, args);
    }
}
