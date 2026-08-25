package profitas.profitas.backend.controller;

import profitas.profitas.backend.dto.OverdraftLoanDTO;
import profitas.profitas.backend.model.OverdraftLoan;
import profitas.profitas.backend.service.OverdraftLoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loans")
public class OverdraftLoanController {

    private final OverdraftLoanService loanService;

    public OverdraftLoanController(OverdraftLoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/apply")
    public ResponseEntity<OverdraftLoan> applyForLoan(@RequestBody OverdraftLoanDTO dto) {
        OverdraftLoan loan = loanService.processLoanApplication(dto);
        return ResponseEntity.ok(loan);
    }
}