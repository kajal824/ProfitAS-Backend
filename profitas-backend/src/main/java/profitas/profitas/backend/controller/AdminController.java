package profitas.profitas.backend.controller;

import jakarta.validation.Valid;
import profitas.profitas.backend.dto.*;
import profitas.profitas.backend.model.enums.ExitStatus;
import profitas.profitas.backend.model.enums.LoanStatus;
import profitas.profitas.backend.service.AssetService;
import profitas.profitas.backend.service.InstantExitService;
import profitas.profitas.backend.repository.UserRepository;
import profitas.profitas.backend.service.OverdraftLoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** All routes under /api/v1/admin require the ADMIN role (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AssetService assetService;
    private final OverdraftLoanService loanService;
    private final InstantExitService exitService;
    private final UserRepository userRepository;

    public AdminController(AssetService assetService, OverdraftLoanService loanService,
                           InstantExitService exitService, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.assetService = assetService;
        this.loanService = loanService;
        this.exitService = exitService;
    }

    // users (so an admin can find the ownerId to attach an asset to)
    @GetMapping("/users")
    public List<UserResponse> allUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    // assets
    @PostMapping("/assets")
    public ResponseEntity<AssetResponse> createAsset(@Valid @RequestBody AssetCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.create(req));
    }

    @GetMapping("/assets")
    public List<AssetResponse> allAssets() {
        return assetService.listAll();
    }

    @PutMapping("/assets/{id}/valuation")
    public AssetResponse updateValuation(@PathVariable Long id, @Valid @RequestBody ValuationUpdateRequest req) {
        return assetService.updateValuation(id, req.getCurrentValuation());
    }

    // loans
    @GetMapping("/loans")
    public List<LoanResponse> allLoans(@RequestParam(required = false) LoanStatus status) {
        return loanService.listAll(status);
    }

    @PostMapping("/loans/{id}/default")
    public LoanResponse defaultLoan(@PathVariable Long id) {
        return loanService.markDefaulted(id);
    }

    // exits
    @GetMapping("/exits")
    public List<ExitResponse> allExits(@RequestParam(required = false) ExitStatus status) {
        return exitService.listAll(status);
    }

    @PostMapping("/exits/{id}/approve")
    public ExitResponse approve(@PathVariable Long id) {
        return exitService.approve(id);
    }

    @PostMapping("/exits/{id}/reject")
    public ExitResponse reject(@PathVariable Long id) {
        return exitService.reject(id);
    }

    @PostMapping("/exits/{id}/settle")
    public ExitResponse settle(@PathVariable Long id) {
        return exitService.settle(id);
    }
}
