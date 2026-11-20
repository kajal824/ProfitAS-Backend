package profitas.profitas.backend.controller;

import jakarta.validation.Valid;
import profitas.profitas.backend.dto.ExitResponse;
import profitas.profitas.backend.dto.InstantExitDTO;
import profitas.profitas.backend.service.InstantExitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exit")
public class InstantExitController {

    private final InstantExitService exitService;

    public InstantExitController(InstantExitService exitService) {
        this.exitService = exitService;
    }

    @PostMapping("/request")
    public ResponseEntity<ExitResponse> createExitRequest(Authentication auth, @Valid @RequestBody InstantExitDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exitService.processInstantExit(auth.getName(), dto));
    }

    @GetMapping
    public List<ExitResponse> myExits(Authentication auth) {
        return exitService.listForUser(auth.getName());
    }
}
