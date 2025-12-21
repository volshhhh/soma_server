package com.soma.chat.config;

import com.soma.chat.model.entity.User;
import com.soma.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Data initializer for creating default users on application startup.
 * Creates misha and dima users for testing/demo purposes.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /**
     * Initialize default users in the database if they don't exist.
     */
    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("Checking for initial users...");

            // Create misha if not exists
            if (userRepository.findByUsername("misha").isEmpty()) {
                User misha = new User();
                misha.setUsername("misha");
                misha.setPassword(passwordEncoder.encode("misha123"));
                misha.setDisplayName("Миша");
                misha.setEmail("misha@soma.com");
                misha.setOnline(false);
                userRepository.save(misha);
                log.info("Created user: misha");
            } else {
                log.info("User misha already exists");
            }

            // Create dima if not exists
            if (userRepository.findByUsername("dima").isEmpty()) {
                User dima = new User();
                dima.setUsername("dima");
                dima.setPassword(passwordEncoder.encode("dima123"));
                dima.setDisplayName("Дима");
                dima.setEmail("dima@soma.com");
                dima.setOnline(false);
                userRepository.save(dima);
                log.info("Created user: dima");
            } else {
                log.info("User dima already exists");
            }

            // Create sasha if not exists
            if (userRepository.findByUsername("sasha").isEmpty()) {
                User sasha = new User();
                sasha.setUsername("sasha");
                sasha.setPassword(passwordEncoder.encode("sasha123"));
                sasha.setDisplayName("Саша");
                sasha.setEmail("sasha@soma.com");
                sasha.setOnline(false);
                userRepository.save(sasha);
                log.info("Created user: sasha");
            } else {
                log.info("User sasha already exists");
            }

            log.info("User initialization complete. Total users: {}", userRepository.count());
        };
    }
}
