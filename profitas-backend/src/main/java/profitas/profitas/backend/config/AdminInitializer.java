package profitas.profitas.backend.config;

import profitas.profitas.backend.model.User;
import profitas.profitas.backend.model.enums.Role;
import profitas.profitas.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first ADMIN from ADMIN_EMAIL / ADMIN_PASSWORD if it doesn't exist. */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            @Value("${app.admin.email:}") String email,
                            @Value("${app.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            return;
        }
        userRepository.save(User.builder()
                .email(normalized)
                .password(passwordEncoder.encode(password))
                .fullName("Administrator")
                .role(Role.ADMIN)
                .build());
        log.info("Bootstrap admin created: {}", normalized);
    }
}
