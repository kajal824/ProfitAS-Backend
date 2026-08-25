package profitas.profitas.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InstantExitDTO {
    private Long assetId;
    private BigDecimal originalValuation;
}