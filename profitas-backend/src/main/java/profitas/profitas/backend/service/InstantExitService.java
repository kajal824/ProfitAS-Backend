package profitas.profitas.backend.service;

import profitas.profitas.backend.dto.InstantExitDTO;
import profitas.profitas.backend.model.FractionalAsset;
import profitas.profitas.backend.model.InstantExitRequest;
import profitas.profitas.backend.model.enums.ExitStatus;
import profitas.profitas.backend.repository.FractionalAssetRepository;
import profitas.profitas.backend.repository.InstantExitRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class InstantExitService {

    @Autowired
    private InstantExitRequestRepository exitRepository;

    @Autowired
    private FractionalAssetRepository assetRepository;

    public InstantExitRequest processInstantExit(InstantExitDTO dto) {
        FractionalAsset asset = assetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        BigDecimal haircut = new BigDecimal("3.00"); // 3% Haircut
        BigDecimal payout = dto.getOriginalValuation()
                .multiply(new BigDecimal("1").subtract(haircut.divide(new BigDecimal("100"))));

        InstantExitRequest exitRequest = InstantExitRequest.builder()
                .asset(asset)
                .originalValuation(dto.getOriginalValuation())
                .haircutPercentage(haircut)
                .payoutAmount(payout)
                .status(ExitStatus.SETTLED)
                .build();

        return exitRepository.save(exitRequest);
    }
}