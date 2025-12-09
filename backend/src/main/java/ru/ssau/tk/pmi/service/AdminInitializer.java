package ru.ssau.tk.pmi.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class AdminInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createAdminUser() {
        try {
            String adminUsername = "admin";
            Optional<User> existingAdmin = userRepository.findByUsername(adminUsername);

            if (existingAdmin.isEmpty()) {
                User admin = new User();
                admin.setUsername(adminUsername);
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");
                admin.setEnabled(true);
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());

                User savedAdmin = userRepository.save(admin);
                logger.info("Создан администратор по умолчанию: {}", savedAdmin.getUsername());
            } else {
                logger.info("Администратор уже существует: {}", adminUsername);
            }
        } catch (Exception e) {
            logger.error("Ошибка при создании администратора: {}", e.getMessage());
        }
    }
}
