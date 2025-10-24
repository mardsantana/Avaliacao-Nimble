package br.com.gatewaynimble.avaliacao_nimble.payment.app;

import br.com.gatewaynimble.avaliacao_nimble.payment.records.PaymentResponse;
import br.com.gatewaynimble.avaliacao_nimble.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/{transactionId}")
    public ResponseEntity<PaymentResponse> refund(@PathVariable UUID transactionId) {
        log.info("[start] RefundController - refund");
        PaymentResponse response = refundService.refund(transactionId);
        log.info("[finish] RefundController - refund");
        return ResponseEntity.ok(response);
    }
}
