package com.srm.mcc.credit.infrastructure.config;

import com.srm.mcc.credit.domain.enums.UserRole;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Seeds default users for local development / H2 only. Never active with the {@code prod}
 * profile — production users must be provisioned manually (see README) with strong,
 * unique passwords.
 */
@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class DevUserSeeder implements CommandLineRunner {

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("admin", "admin123", UserRole.ADMIN);
        seed("operator", "operator123", UserRole.OPERATOR);
        seed("viewer", "viewer123", UserRole.VIEWER);
        log.warn("Dev-only default users seeded (admin/operator/viewer). " +
                "These credentials must NEVER be used outside local development.");
    }

    private void seed(String username, String rawPassword, UserRole role) {
        if (userJpaRepository.findByUsername(username).isPresent()) {
            return;
        }
        userJpaRepository.save(UserJpaEntity.builder()
                .id(UUID.randomUUID())
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
