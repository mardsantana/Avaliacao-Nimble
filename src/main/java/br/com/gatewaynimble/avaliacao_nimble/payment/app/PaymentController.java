package br.com.gatewaynimble.avaliacao_nimble.payment.app;

import br.com.gatewaynimble.avaliacao_nimble.payment.records.DepositRequest;
import br.com.gatewaynimble.avaliacao_nimble.payment.records.PaymentRequest;
import br.com.gatewaynimble.avaliacao_nimble.payment.records.PaymentResponse;
import br.com.gatewaynimble.avaliacao_nimble.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> makePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("[start] PaymentController - makePayment");
        PaymentResponse response = paymentService.processPayment(request);
        log.info("[finish] PaymentController - makePayment");
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/deposit")
    public ResponseEntity<PaymentResponse> deposit(@Valid @RequestBody DepositRequest request) {
        log.info("[start] PaymentController - deposit");
        PaymentResponse response = paymentService.deposit(request);
        log.info("[finish] PaymentController - deposit");
        return ResponseEntity.status(201).body(response);
    }
}
