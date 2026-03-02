package com.cigama.cook_schedule.config;

import com.cigama.cook_schedule.entity.UserAccount;
import com.cigama.cook_schedule.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userAccountRepository.count() == 0) {
            userAccountRepository
                    .save(new UserAccount("admin", "Administrator", passwordEncoder.encode("admin123"), "ADMIN"));
            userAccountRepository.save(new UserAccount("user1", "User One", passwordEncoder.encode("123456"), "USER"));
            userAccountRepository.save(new UserAccount("user2", "User Two", passwordEncoder.encode("123456"), "USER"));
        }
    }
}
