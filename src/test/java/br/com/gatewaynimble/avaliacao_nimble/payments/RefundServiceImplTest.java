package br.com.gatewaynimble.avaliacao_nimble.payments;

import br.com.gatewaynimble.avaliacao_nimble.charge.repository.ChargeRepository;
import br.com.gatewaynimble.avaliacao_nimble.domain.TransactionEntity;
import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.PaymentMethod;
import br.com.gatewaynimble.avaliacao_nimble.exception.BusinessException;
import br.com.gatewaynimble.avaliacao_nimble.exception.ResourceNotFoundException;
import br.com.gatewaynimble.avaliacao_nimble.payment.impl.RefundServiceImpl;
import br.com.gatewaynimble.avaliacao_nimble.payment.records.PaymentResponse;
import br.com.gatewaynimble.avaliacao_nimble.payment.repository.TransactionRepository;
import br.com.gatewaynimble.avaliacao_nimble.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefundServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RefundServiceImpl refundService;

    private User originator;
    private User recipient;
    private TransactionEntity successfulTransaction;
    private final BigDecimal TRANSACTION_AMOUNT = new BigDecimal("100.00");
    private final UUID TRANSACTION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        originator = User.builder()
                .id(UUID.randomUUID()).cpf("1111").fullName("Originator").email("ori@test.com")
                .balance(new BigDecimal("900.00")).ativo(true).createdAt(LocalDateTime.now())
                .build();

        recipient = User.builder()
                .id(UUID.randomUUID()).cpf("2222").fullName("Recipient").email("rec@test.com")
                .balance(new BigDecimal("300.00")).ativo(true).createdAt(LocalDateTime.now())
                .build();

        successfulTransaction = TransactionEntity.builder()
                .id(TRANSACTION_ID).version(0L)
                .originator(originator).recipient(recipient)
                .amount(TRANSACTION_AMOUNT).paymentMethod(PaymentMethod.BALANCE)
                .status(ChargeStatus.SUCCESS).createdAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
            TransactionEntity transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                return TransactionEntity.builder()
                        .id(UUID.randomUUID())
                        .originator(transaction.getOriginator())
                        .recipient(transaction.getRecipient())
                        .amount(transaction.getAmount())
                        .paymentMethod(transaction.getPaymentMethod())
                        .status(transaction.getStatus())
                        .createdAt(transaction.getCreatedAt())
                        .originalTransaction(transaction.getOriginalTransaction())
                        .version(0L)
                        .build();
            }
            return transaction;
        });
    }


    @Test
    @DisplayName("1. Sucesso: Deve processar o reembolso e atualizar saldos e status")
    void shouldProcessRefundSuccessfullyAndUpdateBalancesAndStatus() {
        // ARRANGE
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(successfulTransaction));

        // ACT
        PaymentResponse response = refundService.refund(TRANSACTION_ID);

        // ASSERT
        assertNotNull(response);
        assertEquals(ChargeStatus.REFUNDED, response.status());
        assertEquals(TRANSACTION_AMOUNT, response.amount());
        assertEquals(TRANSACTION_ID, response.originalTransactionId());

        assertEquals(new BigDecimal("200.00"), recipient.getBalance(), "Saldo do Destinatário deve ser reduzido.");
        assertEquals(new BigDecimal("1000.00"), originator.getBalance(), "Saldo do Originador deve ser aumentado.");

        verify(userRepository, times(2)).save(any(User.class)); // 1 para Originador, 1 para Destinatário
        verify(transactionRepository, times(2)).save(any(TransactionEntity.class)); // 1 para Original (REFUNDED), 1 para Reembolso (nova)

        assertEquals(ChargeStatus.REFUNDED, successfulTransaction.getStatus(), "Status da transação original deve ser REFUNDED.");

        ArgumentCaptor<TransactionEntity> newRefundCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, times(2)).save(newRefundCaptor.capture());

        TransactionEntity newRefund = newRefundCaptor.getValue();
        assertEquals(ChargeStatus.REFUNDED, newRefund.getStatus());
        assertEquals(recipient, newRefund.getOriginator(), "A origem do reembolso deve ser o destinatário original.");
        assertEquals(originator, newRefund.getRecipient(), "O destino do reembolso deve ser o originador original.");
        assertEquals(TRANSACTION_ID, newRefund.getOriginalTransaction().getId(), "A transação de reembolso deve referenciar a original.");
    }


    @Test
    @DisplayName("2. Falha: Transação original não encontrada")
    void shouldThrowExceptionWhenOriginalTransactionNotFound() {
        // ARRANGE
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(ResourceNotFoundException.class, () ->
                refundService.refund(TRANSACTION_ID), "Deve lançar ResourceNotFoundException se a transação não existir."
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("3. Falha: Transação com status diferente de SUCCESS")
    void shouldThrowExceptionWhenOriginalTransactionStatusIsNotSuccess() {
        // ARRANGE
        successfulTransaction.setStatus(ChargeStatus.FAILED);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(successfulTransaction));

        // ACT & ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () ->
                refundService.refund(TRANSACTION_ID)
        );

        assertEquals("Apenas transações concluídas podem ser reembolsadas.", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("4. Falha: Destinatário não possui saldo suficiente para cobrir o reembolso")
    void shouldThrowExceptionWhenRecipientHasInsufficientBalance() {
        // ARRANGE
        recipient.setBalance(new BigDecimal("50.00"));
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(successfulTransaction));

        // ACT & ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () ->
                refundService.refund(TRANSACTION_ID)
        );

        assertEquals("O destinatário não possui saldo suficiente para reembolso.", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }
}

