package main.twinbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.twinbackend.entity.UserAccount;
import main.twinbackend.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userAccountRepository.existsByUsername("admin")) {
            UserAccount adminAccount = UserAccount.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .role("ADMIN")
                    .build();
            userAccountRepository.save(adminAccount);
            log.info("Initialized default admin account into database with BCrypt encoded password.");
        }
    }
}
