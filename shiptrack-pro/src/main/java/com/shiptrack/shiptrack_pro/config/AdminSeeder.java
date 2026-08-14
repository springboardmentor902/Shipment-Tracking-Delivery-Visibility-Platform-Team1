package com.shiptrack.shiptrack_pro.config;
 
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
 
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {
 
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
 
    @Override
    public void run(String... args) {
        String adminEmail = "admin@shiptrack.com";
 
        if (userRepository.existsByRole("ADMINISTRATOR")) {
            return;
        }

        User admin = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseGet(() -> User.builder()
                        .fullName("System Administrator")
                        .email(adminEmail)
                        .phone("0000000000")
                        .build());

        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setRole("ADMINISTRATOR");
        admin.setStatus("ACTIVE");
 
        userRepository.save(admin);
        System.out.println("Seeded default admin account: " + adminEmail);
    }
}
