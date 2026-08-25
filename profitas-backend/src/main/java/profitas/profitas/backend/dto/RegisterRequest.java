package profitas.profitas.backend.dto;

import profitas.profitas.backend.model.enums.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private Role role;
}