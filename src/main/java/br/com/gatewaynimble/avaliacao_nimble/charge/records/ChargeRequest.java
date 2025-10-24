package br.com.gatewaynimble.avaliacao_nimble.charge.records;

import br.com.gatewaynimble.avaliacao_nimble.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ChargeRequest(
        @NotBlank String originatorCpf,
        @NotBlank String recipientCpf,
        @NotNull BigDecimal amount,
        @NotNull PaymentMethod paymentMethod
) {}
