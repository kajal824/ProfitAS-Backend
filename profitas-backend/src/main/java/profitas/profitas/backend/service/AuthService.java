package profitas.profitas.backend.service;

import profitas.profitas.backend.dto.AuthRequest;
import profitas.profitas.backend.dto.AuthResponse;
import profitas.profitas.backend.dto.RegisterRequest;
import profitas.profitas.backend.exception.AuthenticationFailedException;
import profitas.profitas.backend.exception.ConflictException;
import profitas.profitas.backend.model.User;
import profitas.profitas.backend.model.enums.Role;
import profitas.profitas.backend.repository.UserRepository;
import profitas.profitas.backend.util.JwtUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email address is already in use");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(Role.USER) // never taken from the client
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) { // lost a race on the unique email constraint
            throw new ConflictException("Email address is already in use");
        }

        return new AuthResponse(jwtUtil.generateToken(user.getEmail()), user.getEmail());
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(normalize(request.getEmail()))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationFailedException("Invalid credentials");
        }

        return new AuthResponse(jwtUtil.generateToken(user.getEmail()), user.getEmail());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
