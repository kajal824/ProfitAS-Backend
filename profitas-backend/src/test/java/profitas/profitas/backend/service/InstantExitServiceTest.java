package profitas.profitas.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import profitas.profitas.backend.dto.InstantExitDTO;
import profitas.profitas.backend.exception.BusinessRuleException;
import profitas.profitas.backend.exception.ForbiddenException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstantExitServiceTest {

    @Mock InstantExitRequestRepository exitRepository;
    @Mock FractionalAssetRepository assetRepository;
    @Mock OverdraftLoanRepository loanRepository;
    @Mock UserRepository userRepository;

    FractionalAsset asset;
    InstantExitDTO dto;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(1L).email("alice@example.com").build();
        asset = FractionalAsset.builder().id(10L).owner(owner).status(AssetStatus.ACTIVE)
                .currentValuation(new BigDecimal("1000000.00")).build();
        dto = new InstantExitDTO();
        dto.setAssetId(10L);
    }

    private InstantExitService service(boolean autoSettle) {
        return new InstantExitService(exitRepository, assetRepository, loanRepository, userRepository,
                new BigDecimal("3.00"), autoSettle);
    }

    @Test
    void appliesThreePercentHaircutToServerSideValuationAndSettles() {
        when(assetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(asset));
        when(loanRepository.findByCollateralAssetIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(List.of());
        when(exitRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var res = service(true).processInstantExit("alice@example.com", dto);

        assertEquals(new BigDecimal("970000.00"), res.payoutAmount());
        assertEquals(new BigDecimal("1000000.00"), res.originalValuation());
        assertEquals(ExitStatus.SETTLED, res.status());
        assertNotNull(res.settledAt());
        assertEquals(AssetStatus.EXITED, asset.getStatus());
    }

    @Test
    void staysPendingWhenAutoSettleIsOff() {
        when(assetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(asset));
        when(loanRepository.findByCollateralAssetIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(List.of());
        when(exitRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var res = service(false).processInstantExit("alice@example.com", dto);

        assertEquals(ExitStatus.PENDING, res.status());
        assertNull(res.settledAt());
        assertEquals(AssetStatus.ACTIVE, asset.getStatus());
    }

    @Test
    void rejectsNonOwner() {
        when(assetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(asset));
        assertThrows(ForbiddenException.class, () -> service(true).processInstantExit("mallory@example.com", dto));
    }

    @Test
    void rejectsWhileLoanIsActive() {
        when(assetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(asset));
        when(loanRepository.findByCollateralAssetIdAndStatus(10L, LoanStatus.ACTIVE))
                .thenReturn(List.of(new OverdraftLoan()));
        assertThrows(BusinessRuleException.class, () -> service(true).processInstantExit("alice@example.com", dto));
        verify(exitRepository, never()).save(any());
    }

    @Test
    void rejectsAlreadyExitedAsset() {
        asset.setStatus(AssetStatus.EXITED);
        when(assetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(asset));
        assertThrows(BusinessRuleException.class, () -> service(true).processInstantExit("alice@example.com", dto));
    }
}
