package profitas.profitas.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RepayRequest {
    @NotNull
    @DecimalMin(value = "0.01", message = "must be greater than 0")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal amount;
}
