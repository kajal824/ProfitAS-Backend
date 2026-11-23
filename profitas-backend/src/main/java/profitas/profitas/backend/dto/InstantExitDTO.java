package profitas.profitas.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** The valuation is deliberately NOT accepted from the client; it is read from the asset. */
@Data
public class InstantExitDTO {
    @NotNull
    private Long assetId;
}
