package br.com.gatewaynimble.avaliacao_nimble.user.controller;

import br.com.gatewaynimble.avaliacao_nimble.user.records.UserRequest;
import br.com.gatewaynimble.avaliacao_nimble.user.records.UserResponse;
import br.com.gatewaynimble.avaliacao_nimble.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        log.info("[start] UserController - register");
        UserResponse response = userService.register(request);
        log.info("[finish] UserController - register");
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<UserResponse> findByCpf(@PathVariable String cpf) {
        log.info("[start] UserController - findByCpf");
        UserResponse response = userService.findByCpf(cpf);
        log.info("[finish] UserController - findByCpf");
        return ResponseEntity.ok(response);
    }
}
