package br.com.gatewaynimble.avaliacao_nimble.user.controller;

import br.com.gatewaynimble.avaliacao_nimble.user.records.LoginResponse;
import br.com.gatewaynimble.avaliacao_nimble.user.service.AuthService;
import br.com.gatewaynimble.avaliacao_nimble.user.records.LoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("[start] AuthController - login");
        String token = authService.login(request);
        log.info("[finish] AuthController - login");
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
