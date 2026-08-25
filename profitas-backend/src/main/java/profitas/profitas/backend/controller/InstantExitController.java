package profitas.profitas.backend.controller;

import profitas.profitas.backend.dto.InstantExitDTO;
import profitas.profitas.backend.model.InstantExitRequest;
import profitas.profitas.backend.service.InstantExitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exit")
public class InstantExitController {

    private final InstantExitService exitService;

    public InstantExitController(InstantExitService exitService) {
        this.exitService = exitService;
    }

    @PostMapping("/request")
    public ResponseEntity<InstantExitRequest> createExitRequest(@RequestBody InstantExitDTO dto) {
        InstantExitRequest request = exitService.processInstantExit(dto);
        return ResponseEntity.ok(request);
    }
}