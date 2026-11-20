package profitas.profitas.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Note: no role field. Self-registered users are always USER; admins are bootstrapped via config. */
@Data
public class RegisterRequest {
    @NotBlank
    @Email
    private String email;

    // BCrypt only uses the first 72 bytes, so cap the length.
    @NotBlank
    @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
    private String password;

    @NotBlank
    @Size(max = 120)
    private String fullName;
}
