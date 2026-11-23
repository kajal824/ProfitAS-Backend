package profitas.profitas.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import profitas.profitas.backend.dto.OverdraftLoanDTO;
import profitas.profitas.backend.exception.BusinessRuleException;
import profitas.profitas.backend.exception.ForbiddenException;
import profitas.profitas.backend.model.FractionalAsset;
import profitas.profitas.backend.model.OverdraftLoan;
import profitas.profitas.backend.model.User;
import profitas.profitas.backend.model.enums.LoanStatus;
import profitas.profitas.backend.repository.FractionalAssetRepository;
import profitas.profitas.backend.repository.InstantExitRequestRepository;
import profitas.profitas.backend.repository.OverdraftLoanRepository;
import profitas.profitas.backend.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OverdraftLoanServiceTest {

    @Mock OverdraftLoanRepository loanRepository;
    @Mock FractionalAssetRepository assetRepository;
    @Mock InstantExitRequestRepository exitRepository;
    @Mock UserRepository userRepository;

    OverdraftLoanService service;
    FractionalAsset asset;

    @BeforeEach
    void setUp() {
        service = new OverdraftLoanService(loanRepository, assetRepository, exitRepository, userRepository,
                new BigDecimal("0.70"), new BigDecimal("11.00"));
        User owner = User.builder().id(1L).email("alice@example.com").build();
        asset = FractionalAsset.builder().id(10L).owner(owner)
                .currentValuation(new BigDecimal("1000000.00")).build();
    }

    private OverdraftLoanDTO dto(String amount) {
        OverdraftLoanDTO d = new OverdraftLoanDTO();
        d.setAssetId(10L);
        d.setRequestedAmount(new BigDecimal(amount));
        return d;
    }

    @Test
    void rejectsNonOwner() {
        when(assetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(asset));
        assertThrows(ForbiddenException.class, () -> service.apply("mallory@example.com", dto("100")));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void allowsLoanUpToSeventyPercent() {
        when(assetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(asset));
        when(loanRepository.findByCollateralAssetIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(List.of());
        when(loanRepository.save(any())).thenAnswer(i -> {
            OverdraftLoan l = i.getArgument(0);
            l.setId(1L);
            return l;
        });

        var res = service.apply("alice@example.com", dto("700000.00"));

        assertEquals(new BigDecimal("700000.00"), res.remainingBalance());
        assertEquals(LoanStatus.ACTIVE, res.status());
    }

    @Test
    void rejectsLoanAboveSeventyPercent() {
        when(assetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(asset));
        when(loanRepository.findByCollateralAssetIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(List.of());
        assertThrows(BusinessRuleException.class, () -> service.apply("alice@example.com", dto("700000.01")));
    }

    @Test
    void limitCountsExistingLoans() {
        OverdraftLoan existing = OverdraftLoan.builder().id(5L).collateralAsset(asset)
                .interestRate(new BigDecimal("11.00")).remainingBalance(new BigDecimal("600000.00"))
                .status(LoanStatus.ACTIVE).lastAccrualAt(LocalDateTime.now()).build();
        when(assetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(asset));
        when(loanRepository.findByCollateralAssetIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(List.of(existing));

        assertThrows(BusinessRuleException.class, () -> service.apply("alice@example.com", dto("100000.01")));
    }

    @Test
    void blocksLoanWhileExitInProgress() {
        when(assetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(asset));
        when(exitRepository.existsByAssetIdAndStatusIn(eq(10L), any())).thenReturn(true);
        assertThrows(BusinessRuleException.class, () -> service.apply("alice@example.com", dto("100")));
    }

    @Test
    void accruesDailyCompoundedInterestForWholeDays() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 12, 0);
        OverdraftLoan loan = OverdraftLoan.builder().interestRate(new BigDecimal("36.50"))
                .remainingBalance(new BigDecimal("1000.00")).status(LoanStatus.ACTIVE)
                .lastAccrualAt(start).build();

        service.accrue(loan, start.plusDays(10).plusHours(5));

        // 1000 * (1 + 0.365/365)^10 = 1010.045... -> 1010.05
        assertEquals(new BigDecimal("1010.05"), loan.getRemainingBalance());
        assertEquals(start.plusDays(10), loan.getLastAccrualAt());
    }

    @Test
    void doesNotAccrueWithinTheSameDay() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 12, 0);
        OverdraftLoan loan = OverdraftLoan.builder().interestRate(new BigDecimal("11.00"))
                .remainingBalance(new BigDecimal("1000.00")).status(LoanStatus.ACTIVE)
                .lastAccrualAt(start).build();

        service.accrue(loan, start.plusHours(23));

        assertEquals(new BigDecimal("1000.00"), loan.getRemainingBalance());
    }

    @Test
    void repayRejectsOverpayment() {
        OverdraftLoan loan = OverdraftLoan.builder().id(1L).collateralAsset(asset)
                .interestRate(new BigDecimal("11.00")).remainingBalance(new BigDecimal("100.00"))
                .status(LoanStatus.ACTIVE).lastAccrualAt(LocalDateTime.now()).build();
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        assertThrows(BusinessRuleException.class,
                () -> service.repay("alice@example.com", 1L, new BigDecimal("100.01")));
    }

    @Test
    void fullRepaymentMarksLoanRepaid() {
        OverdraftLoan loan = OverdraftLoan.builder().id(1L).collateralAsset(asset)
                .interestRate(new BigDecimal("11.00")).remainingBalance(new BigDecimal("100.00"))
                .status(LoanStatus.ACTIVE).lastAccrualAt(LocalDateTime.now()).build();
        when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

        var res = service.repay("alice@example.com", 1L, new BigDecimal("100.00"));

        assertEquals(LoanStatus.REPAID, res.status());
        assertEquals(0, res.remainingBalance().signum());
    }
}
