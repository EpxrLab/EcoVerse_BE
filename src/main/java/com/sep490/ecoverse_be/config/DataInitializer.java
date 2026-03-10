package com.sep490.ecoverse_be.config;

import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminAccount();
    }

    private void initAdminAccount() {
        String adminEmail = "ecoversesep490@gmail.com";

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin account already exists, skipping initialization.");
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setUsername("admin_system");
        admin.setPasswordHash(passwordEncoder.encode("SP26@sep490"));
        admin.setRole(Role.ADMINISTRATOR);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setIsActive(true);

        userRepository.save(admin);
        log.info("Admin account created: {}", adminEmail);
    }
}
