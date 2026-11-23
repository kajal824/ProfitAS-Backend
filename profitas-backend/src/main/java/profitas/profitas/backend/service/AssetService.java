package profitas.profitas.backend.service;

import profitas.profitas.backend.dto.AssetCreateRequest;
import profitas.profitas.backend.dto.AssetResponse;
import profitas.profitas.backend.exception.ForbiddenException;
import profitas.profitas.backend.exception.ResourceNotFoundException;
import profitas.profitas.backend.model.FractionalAsset;
import profitas.profitas.backend.model.User;
import profitas.profitas.backend.model.enums.AssetStatus;
import profitas.profitas.backend.repository.FractionalAssetRepository;
import profitas.profitas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Assets are onboarded by admins only. If users could create their own assets they could
 * invent a valuation and borrow against it.
 */
@Service
public class AssetService {

    private final FractionalAssetRepository assetRepository;
    private final UserRepository userRepository;

    public AssetService(FractionalAssetRepository assetRepository, UserRepository userRepository) {
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AssetResponse create(AssetCreateRequest req) {
        User owner = userRepository.findById(req.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.getOwnerId()));
        FractionalAsset asset = FractionalAsset.builder()
                .assetName(req.getAssetName().trim())
                .currentValuation(money(req.getCurrentValuation()))
                .monthlyRentalIncome(money(req.getMonthlyRentalIncome()))
                .status(AssetStatus.ACTIVE)
                .owner(owner)
                .build();
        return AssetResponse.from(assetRepository.save(asset));
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> listForOwner(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return assetRepository.findByOwnerId(user.getId()).stream().map(AssetResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> listAll() {
        return assetRepository.findAll().stream().map(AssetResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AssetResponse getForOwner(String email, Long assetId) {
        FractionalAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
        if (!asset.getOwner().getEmail().equals(email)) {
            throw new ForbiddenException("You do not own this asset");
        }
        return AssetResponse.from(asset);
    }

    @Transactional
    public AssetResponse updateValuation(Long assetId, BigDecimal valuation) {
        FractionalAsset asset = assetRepository.findByIdForUpdate(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
        asset.setCurrentValuation(money(valuation));
        return AssetResponse.from(asset);
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
