package profitas.profitas.backend.dto;

import profitas.profitas.backend.model.FractionalAsset;
import profitas.profitas.backend.model.enums.AssetStatus;

import java.math.BigDecimal;

public record AssetResponse(Long id, String assetName, BigDecimal currentValuation,
                            BigDecimal monthlyRentalIncome, AssetStatus status, Long ownerId) {
    public static AssetResponse from(FractionalAsset a) {
        return new AssetResponse(a.getId(), a.getAssetName(), a.getCurrentValuation(),
                a.getMonthlyRentalIncome(), a.getStatus(), a.getOwner().getId());
    }
}
