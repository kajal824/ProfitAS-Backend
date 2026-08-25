package profitas.profitas.backend.service;

import profitas.profitas.backend.dto.OverdraftLoanDTO;
import profitas.profitas.backend.model.FractionalAsset;
import profitas.profitas.backend.model.OverdraftLoan;
import profitas.profitas.backend.model.enums.LoanStatus;
import profitas.profitas.backend.repository.FractionalAssetRepository;
import profitas.profitas.backend.repository.OverdraftLoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OverdraftLoanService {

    @Autowired
    private OverdraftLoanRepository loanRepository;

    @Autowired
    private FractionalAssetRepository assetRepository;

    public OverdraftLoan processLoanApplication(OverdraftLoanDTO dto) {
        FractionalAsset asset = assetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        // Maximum allowed loan amount is 70% LTV of current valuation
        BigDecimal maxLoanAllowed = asset.getCurrentValuation().multiply(new BigDecimal("0.70"));

        if (dto.getRequestedAmount().compareTo(maxLoanAllowed) > 0) {
            throw new IllegalArgumentException("Requested loan exceeds maximum 70% LTV threshold of " + maxLoanAllowed);
        }

        OverdraftLoan loan = OverdraftLoan.builder()
                .collateralAsset(asset)
                .loanAmount(dto.getRequestedAmount())
                .interestRate(new BigDecimal("11.00")) // 11% p.a.
                .remainingBalance(dto.getRequestedAmount())
                .status(LoanStatus.ACTIVE)
                .build();

        return loanRepository.save(loan);
    }
}