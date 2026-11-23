package profitas.profitas.backend.dto;

import profitas.profitas.backend.model.OverdraftLoan;
import profitas.profitas.backend.model.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanResponse(Long id, Long assetId, BigDecimal loanAmount, BigDecimal interestRate,
                           BigDecimal remainingBalance, LoanStatus status, LocalDateTime createdAt) {
    public static LoanResponse from(OverdraftLoan l) {
        return new LoanResponse(l.getId(), l.getCollateralAsset().getId(), l.getLoanAmount(),
                l.getInterestRate(), l.getRemainingBalance(), l.getStatus(), l.getCreatedAt());
    }
}
