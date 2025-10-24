package br.com.gatewaynimble.avaliacao_nimble.user.records;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String identifier,
        @NotBlank String password
) {}