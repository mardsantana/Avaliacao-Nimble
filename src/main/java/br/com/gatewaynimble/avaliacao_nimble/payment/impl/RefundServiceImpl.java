package br.com.gatewaynimble.avaliacao_nimble.payment.impl;

import br.com.gatewaynimble.avaliacao_nimble.domain.TransactionEntity;
import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus;
import br.com.gatewaynimble.avaliacao_nimble.exception.BusinessException;
import br.com.gatewaynimble.avaliacao_nimble.exception.ResourceNotFoundException;
import br.com.gatewaynimble.avaliacao_nimble.payment.records.PaymentResponse;
import br.com.gatewaynimble.avaliacao_nimble.payment.repository.TransactionRepository;
import br.com.gatewaynimble.avaliacao_nimble.payment.service.RefundService;
import br.com.gatewaynimble.avaliacao_nimble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PaymentResponse refund(UUID transactionId) {
        log.info("[start] RefundServiceImpl - refund");

        TransactionEntity original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));

        if (original.getStatus() != ChargeStatus.SUCCESS) {
            throw new BusinessException("Apenas transações concluídas podem ser reembolsadas.");
        }

        User originator = original.getOriginator();
        User recipient = original.getRecipient();

        if (recipient.getBalance().compareTo(original.getAmount()) < 0) {
            throw new BusinessException("O destinatário não possui saldo suficiente para reembolso.");
        }

        recipient.setBalance(recipient.getBalance().subtract(original.getAmount()));
        originator.setBalance(originator.getBalance().add(original.getAmount()));
        userRepository.save(originator);
        userRepository.save(recipient);

        original.setStatus(ChargeStatus.REFUNDED);
        transactionRepository.save(original);

        log.info("Transação original marcada como REFUNDED: {}", original.getId());

        TransactionEntity refund = TransactionEntity.builder()
                .originator(recipient)
                .recipient(originator)
                .amount(original.getAmount())
                .paymentMethod(original.getPaymentMethod())
                .status(ChargeStatus.REFUNDED)
                .createdAt(LocalDateTime.now())
                .originalTransaction(original)
                .version(0L)
                .build();

        transactionRepository.save(refund);

        log.info("[finish] RefundServiceImpl - refund");

        return new PaymentResponse(
                refund.getId(),
                refund.getOriginator().getCpf(),
                refund.getRecipient().getCpf(),
                refund.getAmount(),
                refund.getPaymentMethod(),
                refund.getStatus(),
                refund.getCreatedAt(),
                original.getId()
        );
    }
}
