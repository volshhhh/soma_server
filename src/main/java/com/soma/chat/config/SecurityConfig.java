package com.soma.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Конфигурация Spring Security.
 * 
 * Для демонстрации используются in-memory пользователи.
 * В продакшене необходимо заменить на реальную аутентификацию
 * (JWT, OAuth2, сессии с БД и т.д.)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Настройка цепочки фильтров безопасности.
     * 
     * Разрешает:
     * - Доступ к WebSocket эндпоинту
     * - Доступ к статическим ресурсам и H2 консоли
     * - Требует аутентификацию для остальных запросов
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Отключаем CSRF для упрощения (в продакшене нужно настроить корректно)
            .csrf(csrf -> csrf.disable())
            
            // Настройка CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Разрешаем фреймы для H2 консоли
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            
            // Настройка авторизации запросов
            .authorizeHttpRequests(auth -> auth
                // WebSocket эндпоинты доступны всем (аутентификация через STOMP)
                .requestMatchers("/ws-chat/**").permitAll()
                // Статические ресурсы
                .requestMatchers("/", "/index.html", "/css/**", "/js/**").permitAll()
                // H2 консоль (только для разработки!)
                .requestMatchers("/h2-console/**").permitAll()
                // REST API для чата требует аутентификации
                .requestMatchers("/api/**").authenticated()
                // Все остальные запросы требуют аутентификации
                .anyRequest().authenticated()
            )
            
            // Используем HTTP Basic для простоты демонстрации
            // В продакшене заменить на JWT или OAuth2
            .httpBasic(basic -> {})
            
            // Форма логина для браузера
            .formLogin(form -> form
                .defaultSuccessUrl("/index.html", true)
                .permitAll()
            )
            
            .logout(logout -> logout.permitAll());

        return http.build();
    }

    /**
     * Настройка CORS для разрешения запросов с фронтенда.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow all origins for development and ngrok tunneling
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Энкодер паролей.
     * BCrypt - стандартный выбор для хеширования паролей.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // UserDetailsService is now provided by DatabaseUserDetailsService
    // Users are loaded from the database instead of in-memory
}
