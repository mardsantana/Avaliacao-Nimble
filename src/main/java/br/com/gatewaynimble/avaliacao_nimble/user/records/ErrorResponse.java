package br.com.gatewaynimble.avaliacao_nimble.user.records;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp
) {}
