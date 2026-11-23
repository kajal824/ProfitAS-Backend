package profitas.profitas.backend.controller;

import jakarta.validation.Valid;
import profitas.profitas.backend.dto.LoanResponse;
import profitas.profitas.backend.dto.OverdraftLoanDTO;
import profitas.profitas.backend.dto.RepayRequest;
import profitas.profitas.backend.service.OverdraftLoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
public class OverdraftLoanController {

    private final OverdraftLoanService loanService;

    public OverdraftLoanController(OverdraftLoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/apply")
    public ResponseEntity<LoanResponse> applyForLoan(Authentication auth, @Valid @RequestBody OverdraftLoanDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.apply(auth.getName(), dto));
    }

    @GetMapping
    public List<LoanResponse> myLoans(Authentication auth) {
        return loanService.listForUser(auth.getName());
    }

    @GetMapping("/{id}")
    public LoanResponse getLoan(Authentication auth, @PathVariable Long id) {
        return loanService.getForUser(auth.getName(), id);
    }

    @PostMapping("/{id}/repay")
    public LoanResponse repay(Authentication auth, @PathVariable Long id, @Valid @RequestBody RepayRequest req) {
        return loanService.repay(auth.getName(), id, req.getAmount());
    }
}
