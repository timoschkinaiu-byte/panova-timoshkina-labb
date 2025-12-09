package ru.ssau.tk.pmi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.ssau.tk.pmi.entity.User;
import ru.ssau.tk.pmi.repository.UserRepository;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("Попытка загрузки пользователя: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден: {}", username);
                    return new UsernameNotFoundException("Пользователь не найден: " + username);
                });

        // Проверяем, активен ли пользователь
        if (!user.getEnabled()) {
            logger.warn("Пользователь отключен: {}", username);
            throw new UsernameNotFoundException("Пользователь отключен: " + username);
        }

        // Создаем authorities из роли (добавляем префикс ROLE_ для Spring Security)
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());

        logger.info("Пользователь успешно загружен: {}, роль: {}", username, user.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                Collections.singletonList(authority)
        );
    }
}
