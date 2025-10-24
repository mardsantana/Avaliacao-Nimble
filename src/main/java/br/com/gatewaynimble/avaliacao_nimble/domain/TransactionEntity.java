package br.com.gatewaynimble.avaliacao_nimble.domain;

import br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "originator_id")
    private User originator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Campo opcional para vincular o reembolso à transação original.
     * Exemplo: se o usuário fez um refund, este campo aponta para a transação original.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_transaction_id")
    private TransactionEntity originalTransaction;

    // ==== Métodos de negócio (opcional, mas recomendável) ====
    public boolean isRefundable() {
        return this.status == ChargeStatus.SUCCESS;
    }

    public void markAsRefunded() {
        this.status = ChargeStatus.REFUNDED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

