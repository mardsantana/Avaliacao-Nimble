package br.com.gatewaynimble.avaliacao_nimble.user.records;

import java.math.BigDecimal;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String cpf,
        String email,
        String fullName,
        BigDecimal balance
) {}
