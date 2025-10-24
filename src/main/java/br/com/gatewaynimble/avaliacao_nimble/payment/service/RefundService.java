package br.com.gatewaynimble.avaliacao_nimble.payment.service;


import br.com.gatewaynimble.avaliacao_nimble.payment.records.PaymentResponse;

import java.util.UUID;

public interface RefundService {
    PaymentResponse refund(UUID transactionId);
}