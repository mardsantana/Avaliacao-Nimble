package br.com.gatewaynimble.avaliacao_nimble.payment.impl;

import br.com.gatewaynimble.avaliacao_nimble.authorizer.AuthorizerClient;
import br.com.gatewaynimble.avaliacao_nimble.charge.repository.ChargeRepository;
import br.com.gatewaynimble.avaliacao_nimble.domain.Charge;
import br.com.gatewaynimble.avaliacao_nimble.domain.TransactionEntity;
import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus;
import br.com.gatewaynimble.avaliacao_nimble.exception.BusinessException;
import br.com.gatewaynimble.avaliacao_nimble.exception.ResourceNotFoundException;
import br.com.gatewaynimble.avaliacao_nimble.payment.records.DepositRequest;
import br.com.gatewaynimble.avaliacao_nimble.payment.records.PaymentRequest;
import br.com.gatewaynimble.avaliacao_nimble.payment.records.PaymentResponse;
import br.com.gatewaynimble.avaliacao_nimble.payment.repository.TransactionRepository;
import br.com.gatewaynimble.avaliacao_nimble.payment.service.PaymentService;
import br.com.gatewaynimble.avaliacao_nimble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final UserRepository userRepository;
    private final ChargeRepository chargeRepository;
    private final TransactionRepository transactionRepository;
    private final AuthorizerClient authorizerClient;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("[start] PaymentServiceImpl - processPayment");

        // Buscar usuários
        User originator = userRepository.findByCpf(request.originatorCpf())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário originador não encontrado"));

        User recipient = userRepository.findByCpf(request.recipientCpf())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário destinatário não encontrado"));

        if (!originator.isAtivo() || !recipient.isAtivo()) {
            throw new BusinessException("Um dos usuários está inativo e não pode realizar transações.");
        }

        // 🔐 Validação via autorizador externo (para cartão)
        if ("CARD".equalsIgnoreCase(request.paymentMethod().name())) {
            boolean approved = authorizerClient.isApproved();
            if (!approved) {
                // Se houver cobrança, marcar como FAILED
                markChargeFailedIfExists(originator, recipient, request.amount());
                throw new BusinessException("Transação negada pelo autorizador externo.");
            }
        }

        // 💰 Validação de saldo (para pagamentos via BALANCE)
        if ("BALANCE".equalsIgnoreCase(request.paymentMethod().name()) &&
                originator.getBalance().compareTo(request.amount()) < 0) {
            markChargeFailedIfExists(originator, recipient, request.amount());
            throw new BusinessException("Saldo insuficiente para realizar o pagamento.");
        }

        // Atualizar saldos
        originator.setBalance(originator.getBalance().subtract(request.amount()));
        recipient.setBalance(recipient.getBalance().add(request.amount()));
        userRepository.save(originator);
        userRepository.save(recipient);

        // Buscar a cobrança PENDING correspondente (se existir)
        Charge charge = chargeRepository.findFirstByOriginatorAndRecipientAndAmountAndStatus(
                originator, recipient, request.amount(), ChargeStatus.PENDING
        ).orElse(null);

        if (charge != null) {
            charge.markAsPaid(recipient);
            chargeRepository.save(charge);
            log.info("Cobrança marcada como SUCCESS: {}", charge.getId());
        }

        // Registrar transação
        TransactionEntity transaction = TransactionEntity.builder()
//                .id(UUID.randomUUID())
                .originator(originator)
                .recipient(recipient)
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .status(ChargeStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);

        log.info("[finish] PaymentServiceImpl - processPayment");
        return new PaymentResponse(
                transaction.getId(),
                originator.getCpf(),
                recipient.getCpf(),
                transaction.getAmount(),
                transaction.getPaymentMethod(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                charge != null ? charge.getId() : null // vincula cobrança se existir
        );
    }

    @Override
    @Transactional
    public PaymentResponse deposit(DepositRequest request) {
        log.info("[start] PaymentServiceImpl - deposit");

        User recipient = userRepository.findByCpf(request.recipientCpf())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário destinatário não encontrado"));

        if (!recipient.isAtivo()) {
            throw new BusinessException("Usuário inativo não pode receber depósitos.");
        }

        // 🔐 Validação via autorizador externo
        log.info("Verificando autorização externa para depósito...");
        boolean approved = authorizerClient.isApproved();
        if (!approved) {
            throw new BusinessException("Depósito negado pelo autorizador externo.");
        }

        // Atualiza saldo
        recipient.setBalance(recipient.getBalance().add(request.amount()));
        userRepository.save(recipient);

        TransactionEntity transaction = TransactionEntity.builder()
//                .id(UUID.randomUUID())
                .originator(null)
                .recipient(recipient)
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .status(ChargeStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);

        log.info("[finish] PaymentServiceImpl - deposit");
        return new PaymentResponse(
                transaction.getId(),
                null,
                recipient.getCpf(),
                transaction.getAmount(),
                transaction.getPaymentMethod(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                null
        );
    }


    private void markChargeFailedIfExists(User originator, User recipient, BigDecimal amount) {
        chargeRepository.findFirstByOriginatorAndRecipientAndAmountAndStatus(
                originator, recipient, amount, ChargeStatus.PENDING
        ).ifPresent(charge -> {
            charge.markAsFailed();
            chargeRepository.save(charge);
            log.info("Cobrança marcada como FAILED: {}", charge.getId());
        });
    }

}