package com.openhack.emergencymanager.config;

import com.openhack.emergencymanager.model.AppUser;
import com.openhack.emergencymanager.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Locale;

@Configuration
public class AdminBootstrap {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    @Bean
    public ApplicationRunner initialAdmin(
            AppUserRepository users, PasswordEncoder encoder,
            @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String adminEmail,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String adminPassword) {
        return args -> {
            if (users.countByRole(AppUser.Role.ADMIN) > 0) return;
            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                log.warn("No ADMIN exists. Set BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD before starting.");
                return;
            }
            if (adminPassword.length() < 12 || !adminEmail.contains("@")) {
                throw new IllegalArgumentException("Bootstrap admin needs a valid email and password of 12+ characters");
            }
            String email = adminEmail.trim().toLowerCase(Locale.ROOT);
            users.save(new AppUser("Hospital Administrator", email, encoder.encode(adminPassword),
                AppUser.Role.ADMIN, AppUser.Status.ACTIVE));
            log.info("Initial administrator account created for {}", email);
        };
    }
}
