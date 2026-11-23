package profitas.profitas.backend.controller;

import profitas.profitas.backend.dto.AssetResponse;
import profitas.profitas.backend.service.AssetService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public List<AssetResponse> myAssets(Authentication auth) {
        return assetService.listForOwner(auth.getName());
    }

    @GetMapping("/{id}")
    public AssetResponse getAsset(Authentication auth, @PathVariable Long id) {
        return assetService.getForOwner(auth.getName(), id);
    }
}
