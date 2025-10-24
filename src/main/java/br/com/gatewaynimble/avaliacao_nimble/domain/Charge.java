package br.com.gatewaynimble.avaliacao_nimble.domain;

import br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.PaymentMethod;
import br.com.gatewaynimble.avaliacao_nimble.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_charges")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "originator_id", nullable = false)
    private User originator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(length = 255)
    private String externalAuthId;

    private LocalDateTime paidAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    public void markAsPaid(User payer) {
        if (this.status != ChargeStatus.PENDING) {
            throw new BusinessException("Cobrança já foi processada ou inválida");
        }

        if (!payer.getId().equals(this.recipient.getId())) {
            throw new BusinessException("Apenas o destinatário pode pagar a cobrança");
        }

        this.status = ChargeStatus.SUCCESS;
        this.paidAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        if (this.status != ChargeStatus.PENDING) {
            throw new BusinessException("Cobrança já foi processada");
        }
        this.status = ChargeStatus.FAILED;
    }

    public void markAsRefunded() {
        if (this.status != ChargeStatus.SUCCESS) {
            throw new BusinessException("Somente cobranças pagas podem ser reembolsadas");
        }
        this.status = ChargeStatus.REFUNDED;
    }
}
