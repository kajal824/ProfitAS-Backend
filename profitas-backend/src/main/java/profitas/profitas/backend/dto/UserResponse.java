package profitas.profitas.backend.dto;

import profitas.profitas.backend.model.User;
import profitas.profitas.backend.model.enums.Role;

public record UserResponse(Long id, String email, String fullName, Role role) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole());
    }
}
