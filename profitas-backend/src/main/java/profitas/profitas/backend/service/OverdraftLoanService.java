package profitas.profitas.backend.service;

import profitas.profitas.backend.dto.LoanResponse;
import profitas.profitas.backend.dto.OverdraftLoanDTO;
import profitas.profitas.backend.exception.BusinessRuleException;
import profitas.profitas.backend.exception.ForbiddenException;
import profitas.profitas.backend.exception.ResourceNotFoundException;
import profitas.profitas.backend.model.FractionalAsset;
import profitas.profitas.backend.model.OverdraftLoan;
import profitas.profitas.backend.model.User;
import profitas.profitas.backend.model.enums.AssetStatus;
import profitas.profitas.backend.model.enums.ExitStatus;
import profitas.profitas.backend.model.enums.LoanStatus;
import profitas.profitas.backend.repository.FractionalAssetRepository;
import profitas.profitas.backend.repository.InstantExitRequestRepository;
import profitas.profitas.backend.repository.OverdraftLoanRepository;
import profitas.profitas.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class OverdraftLoanService {

    private final OverdraftLoanRepository loanRepository;
    private final FractionalAssetRepository assetRepository;
    private final InstantExitRequestRepository exitRepository;
    private final UserRepository userRepository;
    private final BigDecimal maxLtv;
    private final BigDecimal interestRate;

    public OverdraftLoanService(OverdraftLoanRepository loanRepository,
                                FractionalAssetRepository assetRepository,
                                InstantExitRequestRepository exitRepository,
                                UserRepository userRepository,
                                @Value("${app.loan.max-ltv:0.70}") BigDecimal maxLtv,
                                @Value("${app.loan.interest-rate:11.00}") BigDecimal interestRate) {
        this.loanRepository = loanRepository;
        this.assetRepository = assetRepository;
        this.exitRepository = exitRepository;
        this.userRepository = userRepository;
        this.maxLtv = maxLtv;
        this.interestRate = interestRate;
    }

    @Transactional
    public LoanResponse apply(String email, OverdraftLoanDTO dto) {
        FractionalAsset asset = assetRepository.findByIdForUpdate(dto.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + dto.getAssetId()));
        requireOwner(asset, email);

        if (asset.getStatus() != AssetStatus.ACTIVE) {
            throw new BusinessRuleException("Asset has been exited and cannot be used as collateral");
        }
        if (exitRepository.existsByAssetIdAndStatusIn(asset.getId(),
                Set.of(ExitStatus.PENDING, ExitStatus.APPROVED))) {
            throw new BusinessRuleException("Asset has an exit request in progress");
        }

        // The LTV limit applies to everything borrowed against the asset, not just this request.
        LocalDateTime now = LocalDateTime.now();
        BigDecimal outstanding = BigDecimal.ZERO;
        for (OverdraftLoan existing : loanRepository.findByCollateralAssetIdAndStatus(asset.getId(), LoanStatus.ACTIVE)) {
            accrue(existing, now);
            outstanding = outstanding.add(existing.getRemainingBalance());
        }

        BigDecimal maxTotal = asset.getCurrentValuation().multiply(maxLtv).setScale(2, RoundingMode.DOWN);
        BigDecimal available = maxTotal.subtract(outstanding).max(BigDecimal.ZERO);
        BigDecimal requested = dto.getRequestedAmount().setScale(2, RoundingMode.HALF_UP);

        if (requested.compareTo(available) > 0) {
            throw new BusinessRuleException("Requested loan exceeds the available limit of " + available
                    + " (max " + maxTotal + " at " + maxLtv.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString()
                    + "% LTV, outstanding " + outstanding + ")");
        }

        OverdraftLoan loan = OverdraftLoan.builder()
                .collateralAsset(asset)
                .loanAmount(requested)
                .interestRate(interestRate)
                .remainingBalance(requested)
                .status(LoanStatus.ACTIVE)
                .build();

        return LoanResponse.from(loanRepository.save(loan));
    }

    @Transactional
    public List<LoanResponse> listForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        LocalDateTime now = LocalDateTime.now();
        List<OverdraftLoan> loans = loanRepository.findByCollateralAssetOwnerId(user.getId());
        loans.forEach(l -> accrue(l, now));
        return loans.stream().map(LoanResponse::from).toList();
    }

    @Transactional
    public LoanResponse getForUser(String email, Long loanId) {
        OverdraftLoan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
        requireOwner(loan.getCollateralAsset(), email);
        accrue(loan, LocalDateTime.now());
        return LoanResponse.from(loan);
    }

    @Transactional
    public LoanResponse repay(String email, Long loanId, BigDecimal amount) {
        OverdraftLoan loan = loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
        requireOwner(loan.getCollateralAsset(), email);

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new BusinessRuleException("Loan is " + loan.getStatus() + " and cannot be repaid");
        }

        accrue(loan, LocalDateTime.now());
        BigDecimal payment = amount.setScale(2, RoundingMode.HALF_UP);
        if (payment.compareTo(loan.getRemainingBalance()) > 0) {
            throw new BusinessRuleException("Repayment exceeds outstanding balance of " + loan.getRemainingBalance());
        }

        loan.setRemainingBalance(loan.getRemainingBalance().subtract(payment));
        if (loan.getRemainingBalance().signum() == 0) {
            loan.setStatus(LoanStatus.REPAID);
        }
        return LoanResponse.from(loan);
    }

    // ---- admin ----

    @Transactional
    public List<LoanResponse> listAll(LoanStatus status) {
        List<OverdraftLoan> loans = status == null ? loanRepository.findAll() : loanRepository.findByStatus(status);
        LocalDateTime now = LocalDateTime.now();
        loans.forEach(l -> accrue(l, now));
        return loans.stream().map(LoanResponse::from).toList();
    }

    @Transactional
    public LoanResponse markDefaulted(Long loanId) {
        OverdraftLoan loan = loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE loans can be marked as defaulted");
        }
        accrue(loan, LocalDateTime.now());
        loan.setStatus(LoanStatus.DEFAULTED);
        return LoanResponse.from(loan);
    }

    // ---- helpers ----

    private static void requireOwner(FractionalAsset asset, String email) {
        if (!asset.getOwner().getEmail().equals(email)) {
            throw new ForbiddenException("You do not own this asset");
        }
    }

    /**
     * Applies interest for each whole day elapsed since the last accrual, compounded daily at
     * rate/365. Because only whole days are consumed and lastAccrualAt advances by exactly that
     * many days, the result doesn't depend on how often this is called (beyond cent rounding).
     */
    void accrue(OverdraftLoan loan, LocalDateTime now) {
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            return;
        }
        if (loan.getLastAccrualAt() == null) {
            loan.setLastAccrualAt(loan.getCreatedAt() != null ? loan.getCreatedAt() : now);
        }
        long days = ChronoUnit.DAYS.between(loan.getLastAccrualAt(), now);
        if (days <= 0) {
            return;
        }
        BigDecimal daily = loan.getInterestRate().divide(new BigDecimal("36500"), 12, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(daily).pow(Math.toIntExact(days), MathContext.DECIMAL64);
        loan.setRemainingBalance(loan.getRemainingBalance().multiply(factor).setScale(2, RoundingMode.HALF_UP));
        loan.setLastAccrualAt(loan.getLastAccrualAt().plusDays(days));
    }
}
