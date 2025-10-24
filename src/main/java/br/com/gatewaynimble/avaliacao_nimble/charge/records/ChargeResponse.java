package br.com.gatewaynimble.avaliacao_nimble.charge.records;

import br.com.gatewaynimble.avaliacao_nimble.domain.Charge;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChargeResponse(
        UUID id,
        String originatorCpf,
        String recipientCpf,
        BigDecimal amount,
        ChargeStatus status,
        PaymentMethod paymentMethod,
        LocalDateTime createdAt
) {
    public static ChargeResponse from(Charge charge) {
        return new ChargeResponse(
                charge.getId(),
                charge.getOriginator().getCpf(),
                charge.getRecipient().getCpf(),
                charge.getAmount(),
                charge.getStatus(),
                charge.getPaymentMethod(),
                charge.getCreatedAt()
        );
    }
}