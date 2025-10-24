package br.com.gatewaynimble.avaliacao_nimble.payment.records;

import br.com.gatewaynimble.avaliacao_nimble.domain.enums.PaymentMethod;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID transactionId,
        String originatorCpf,
        String recipientCpf,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        ChargeStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        UUID originalTransactionId
) {}
