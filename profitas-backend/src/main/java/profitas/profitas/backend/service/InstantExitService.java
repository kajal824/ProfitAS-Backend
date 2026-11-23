package profitas.profitas.backend.service;

import profitas.profitas.backend.dto.ExitResponse;
import profitas.profitas.backend.dto.InstantExitDTO;
import profitas.profitas.backend.exception.BusinessRuleException;
import profitas.profitas.backend.exception.ForbiddenException;
import profitas.profitas.backend.exception.ResourceNotFoundException;
import profitas.profitas.backend.model.FractionalAsset;
import profitas.profitas.backend.model.InstantExitRequest;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class InstantExitService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final InstantExitRequestRepository exitRepository;
    private final FractionalAssetRepository assetRepository;
    private final OverdraftLoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BigDecimal haircutPercent;
    private final boolean autoSettle;

    public InstantExitService(InstantExitRequestRepository exitRepository,
                              FractionalAssetRepository assetRepository,
                              OverdraftLoanRepository loanRepository,
                              UserRepository userRepository,
                              @Value("${app.exit.haircut-percent:3.00}") BigDecimal haircutPercent,
                              @Value("${app.exit.auto-settle:true}") boolean autoSettle) {
        this.exitRepository = exitRepository;
        this.assetRepository = assetRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.haircutPercent = haircutPercent;
        this.autoSettle = autoSettle;
    }

    @Transactional
    public ExitResponse processInstantExit(String email, InstantExitDTO dto) {
        FractionalAsset asset = assetRepository.findByIdForUpdate(dto.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + dto.getAssetId()));

        if (!asset.getOwner().getEmail().equals(email)) {
            throw new ForbiddenException("You do not own this asset");
        }
        if (asset.getStatus() != AssetStatus.ACTIVE) {
            throw new BusinessRuleException("Asset has already been exited");
        }
        if (exitRepository.existsByAssetIdAndStatusIn(asset.getId(), Set.of(ExitStatus.PENDING, ExitStatus.APPROVED))) {
            throw new BusinessRuleException("An exit request for this asset is already in progress");
        }
        if (!loanRepository.findByCollateralAssetIdAndStatus(asset.getId(), LoanStatus.ACTIVE).isEmpty()) {
            throw new BusinessRuleException("Repay the active loan(s) against this asset before exiting");
        }

        // Valuation comes from the asset record, never from the client.
        BigDecimal valuation = asset.getCurrentValuation();
        BigDecimal payout = valuation
                .multiply(HUNDRED.subtract(haircutPercent))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        InstantExitRequest request = InstantExitRequest.builder()
                .asset(asset)
                .originalValuation(valuation)
                .haircutPercentage(haircutPercent)
                .payoutAmount(payout)
                .status(ExitStatus.PENDING)
                .build();

        if (autoSettle) {
            settleInternal(request, asset);
        }
        return ExitResponse.from(exitRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<ExitResponse> listForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return exitRepository.findByAssetOwnerId(user.getId()).stream().map(ExitResponse::from).toList();
    }

    // ---- admin workflow (used when app.exit.auto-settle=false) ----

    @Transactional(readOnly = true)
    public List<ExitResponse> listAll(ExitStatus status) {
        List<InstantExitRequest> list = status == null ? exitRepository.findAll() : exitRepository.findByStatus(status);
        return list.stream().map(ExitResponse::from).toList();
    }

    @Transactional
    public ExitResponse approve(Long id) {
        InstantExitRequest req = load(id);
        requireStatus(req, ExitStatus.PENDING);
        req.setStatus(ExitStatus.APPROVED);
        return ExitResponse.from(req);
    }

    @Transactional
    public ExitResponse reject(Long id) {
        InstantExitRequest req = load(id);
        if (req.getStatus() != ExitStatus.PENDING && req.getStatus() != ExitStatus.APPROVED) {
            throw new BusinessRuleException("Only PENDING or APPROVED requests can be rejected");
        }
        req.setStatus(ExitStatus.REJECTED);
        return ExitResponse.from(req);
    }

    @Transactional
    public ExitResponse settle(Long id) {
        InstantExitRequest req = load(id);
        requireStatus(req, ExitStatus.APPROVED);
        FractionalAsset asset = assetRepository.findByIdForUpdate(req.getAsset().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        settleInternal(req, asset);
        return ExitResponse.from(req);
    }

    private void settleInternal(InstantExitRequest req, FractionalAsset asset) {
        req.setStatus(ExitStatus.SETTLED);
        req.setSettledAt(LocalDateTime.now());
        asset.setStatus(AssetStatus.EXITED);
    }

    private InstantExitRequest load(Long id) {
        return exitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exit request not found: " + id));
    }

    private static void requireStatus(InstantExitRequest req, ExitStatus expected) {
        if (req.getStatus() != expected) {
            throw new BusinessRuleException("Request must be " + expected + " but is " + req.getStatus());
        }
    }
}
