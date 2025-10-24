package br.com.gatewaynimble.avaliacao_nimble.payment.service;

import br.com.gatewaynimble.avaliacao_nimble.payment.records.DepositRequest;
import br.com.gatewaynimble.avaliacao_nimble.payment.records.PaymentRequest;
import br.com.gatewaynimble.avaliacao_nimble.payment.records.PaymentResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse deposit(DepositRequest request);
}
