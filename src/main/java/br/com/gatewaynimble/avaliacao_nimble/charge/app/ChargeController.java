package br.com.gatewaynimble.avaliacao_nimble.charge.app;

import br.com.gatewaynimble.avaliacao_nimble.charge.service.ChargeQueryService;
import br.com.gatewaynimble.avaliacao_nimble.charge.records.ChargeRequest;
import br.com.gatewaynimble.avaliacao_nimble.charge.records.ChargeResponse;
import br.com.gatewaynimble.avaliacao_nimble.charge.service.ChargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/charges")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;
    private final ChargeQueryService chargeQueryService;

    @PostMapping
    public ResponseEntity<ChargeResponse> create(@Valid @RequestBody ChargeRequest request) {
        log.info("[start] ChargeController - create");
        ChargeResponse response = chargeService.createCharge(request);
        log.info("[finish] ChargeController - create");
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChargeResponse> findById(@PathVariable UUID id) {
        log.info("[start] ChargeController - findById");
        ChargeResponse response = chargeQueryService.findById(id);
        log.info("[finish] ChargeController - findById");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{cpf}")
    public ResponseEntity<List<ChargeResponse>> findByUser(@PathVariable String cpf) {
        log.info("[start] ChargeController - findByUser");
        List<ChargeResponse> response = chargeQueryService.findByUser(cpf);
        log.info("[finish] ChargeController - findByUser");
        return ResponseEntity.ok(response);
    }
}
