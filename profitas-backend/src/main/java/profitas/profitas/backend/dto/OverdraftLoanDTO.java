package profitas.profitas.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OverdraftLoanDTO {
    private Long assetId;
    private BigDecimal requestedAmount;
}