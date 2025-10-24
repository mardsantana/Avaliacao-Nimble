package br.com.gatewaynimble.avaliacao_nimble.payment.records;

import br.com.gatewaynimble.avaliacao_nimble.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(
        @NotBlank String recipientCpf,
        @NotNull BigDecimal amount,
        @NotNull PaymentMethod paymentMethod
) {}