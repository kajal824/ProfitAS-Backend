package profitas.profitas.backend.dto;

import profitas.profitas.backend.model.InstantExitRequest;
import profitas.profitas.backend.model.enums.ExitStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExitResponse(Long id, Long assetId, BigDecimal originalValuation, BigDecimal haircutPercentage,
                           BigDecimal payoutAmount, ExitStatus status, LocalDateTime requestedAt,
                           LocalDateTime settledAt) {
    public static ExitResponse from(InstantExitRequest e) {
        return new ExitResponse(e.getId(), e.getAsset().getId(), e.getOriginalValuation(),
                e.getHaircutPercentage(), e.getPayoutAmount(), e.getStatus(), e.getRequestedAt(),
                e.getSettledAt());
    }
}
