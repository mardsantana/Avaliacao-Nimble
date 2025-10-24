package br.com.gatewaynimble.avaliacao_nimble.payments;

import br.com.gatewaynimble.avaliacao_nimble.authorizer.AuthorizerClient;
import br.com.gatewaynimble.avaliacao_nimble.charge.repository.ChargeRepository;
import br.com.gatewaynimble.avaliacao_nimble.domain.Charge;
import br.com.gatewaynimble.avaliacao_nimble.domain.TransactionEntity;
import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus;
import br.com.gatewaynimble.avaliacao_nimble.exception.BusinessException;
import br.com.gatewaynimble.avaliacao_nimble.exception.ResourceNotFoundException;
import br.com.gatewaynimble.avaliacao_nimble.payment.impl.PaymentServiceImpl;
import br.com.gatewaynimble.avaliacao_nimble.payment.records.DepositRequest;
import br.com.gatewaynimble.avaliacao_nimble.payment.records.PaymentRequest;
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

import static br.com.gatewaynimble.avaliacao_nimble.domain.enums.PaymentMethod.BALANCE;
import static br.com.gatewaynimble.avaliacao_nimble.domain.enums.PaymentMethod.CARD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AuthorizerClient authorizerClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User originator;
    private User recipient;
    private final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");
    private final BigDecimal TRANSACTION_AMOUNT = new BigDecimal("100.00");
    private PaymentRequest validBalanceRequest;
    private PaymentRequest validCardRequest;
    private DepositRequest validDepositRequest;
    private Charge pendingCharge;

    @BeforeEach
    void setUp() {
        originator = User.builder()
                .id(UUID.randomUUID())
                .cpf("11122233344")
                .fullName("Originator")
                .balance(INITIAL_BALANCE)
                .email("ori@test.com").password("pass")
                .ativo(true)
                .createdAt(LocalDateTime.now())
                .build();

        recipient = User.builder()
                .id(UUID.randomUUID())
                .cpf("55566677788")
                .fullName("Recipient")
                .balance(new BigDecimal("200.00"))
                .email("rec@test.com").password("pass")
                .ativo(true)
                .createdAt(LocalDateTime.now())
                .build();

        validBalanceRequest = new PaymentRequest(
                originator.getCpf(), recipient.getCpf(), TRANSACTION_AMOUNT, BALANCE
        );
        validCardRequest = new PaymentRequest(
                originator.getCpf(), recipient.getCpf(), TRANSACTION_AMOUNT, CARD
        );
        validDepositRequest = new DepositRequest(
                recipient.getCpf(), TRANSACTION_AMOUNT, CARD
        );

        pendingCharge = Charge.builder()
                .id(UUID.randomUUID())
                .originator(originator)
                .recipient(recipient)
                .amount(TRANSACTION_AMOUNT)
                .paymentMethod(CARD)
                .status(ChargeStatus.PENDING)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private void mockSuccessfulUserLookup() {
        when(userRepository.findByCpf(originator.getCpf())).thenReturn(Optional.of(originator));
        when(userRepository.findByCpf(recipient.getCpf())).thenReturn(Optional.of(recipient));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void mockSuccessfulTransactionSave() {
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
            TransactionEntity transaction = invocation.getArgument(0);
            return TransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .version(0L)
                    .originator(transaction.getOriginator())
                    .recipient(transaction.getRecipient())
                    .amount(transaction.getAmount())
                    .paymentMethod(transaction.getPaymentMethod())
                    .status(transaction.getStatus())
                    .createdAt(transaction.getCreatedAt() != null ? transaction.getCreatedAt() : LocalDateTime.now())
                    .originalTransaction(transaction.getOriginalTransaction())
                    .build();
        });
    }


    @Test
    @DisplayName("1. Sucesso: Pagamento via BALANCE sem cobrança pendente")
    void shouldProcessPaymentWithBalanceSuccessfully() {
        mockSuccessfulUserLookup();
        mockSuccessfulTransactionSave();
        when(chargeRepository.findFirstByOriginatorAndRecipientAndAmountAndStatus(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        // ACT
        PaymentResponse response = paymentService.processPayment(validBalanceRequest);

        // ASSERT
        assertNotNull(response);
        assertEquals(ChargeStatus.SUCCESS, response.status());
        assertEquals(BALANCE, response.paymentMethod());

        // Verifica saldos
        assertEquals(INITIAL_BALANCE.subtract(TRANSACTION_AMOUNT), originator.getBalance());
        assertEquals(new BigDecimal("300.00"), recipient.getBalance()); // 200.00 + 100.00

        // Verifica saves
        verify(authorizerClient, never()).isApproved();
        verify(userRepository, times(2)).save(any(User.class));
        verify(transactionRepository, times(1)).save(any(TransactionEntity.class));
        verify(chargeRepository, never()).save(any(Charge.class));
    }

    @Test
    @DisplayName("2. Sucesso: Pagamento via CARD com autorização e cobrança pendente")
    void shouldProcessPaymentWithCardAndPendingChargeSuccessfully() {
        mockSuccessfulUserLookup();
        mockSuccessfulTransactionSave();
        when(authorizerClient.isApproved()).thenReturn(true); // Autorização OK
        when(chargeRepository.findFirstByOriginatorAndRecipientAndAmountAndStatus(any(), any(), any(), eq(ChargeStatus.PENDING)))
                .thenReturn(Optional.of(pendingCharge)); // Cobrança pendente

        // ACT
        PaymentResponse response = paymentService.processPayment(validCardRequest);

        // ASSERT
        assertNotNull(response);
        assertEquals(ChargeStatus.SUCCESS, response.status());
        assertEquals(CARD, response.paymentMethod());

        // Verifica saldos
        assertEquals(INITIAL_BALANCE.subtract(TRANSACTION_AMOUNT), originator.getBalance());

        // Verifica se a cobrança foi atualizada
        ArgumentCaptor<Charge> chargeCaptor = ArgumentCaptor.forClass(Charge.class);
        verify(chargeRepository, times(1)).save(chargeCaptor.capture());
        assertEquals(ChargeStatus.SUCCESS, chargeCaptor.getValue().getStatus());

        // Verifica autorizador
        verify(authorizerClient, times(1)).isApproved();
    }

    @Test
    @DisplayName("3. Falha: Saldo insuficiente para pagamento BALANCE")
    void shouldFailPaymentWhenBalanceIsInsufficient() {
        // ARRANGE
        originator.setBalance(new BigDecimal("50.00"));
        mockSuccessfulUserLookup();

        when(chargeRepository.findFirstByOriginatorAndRecipientAndAmountAndStatus(any(), any(), any(), eq(ChargeStatus.PENDING)))
                .thenReturn(Optional.of(pendingCharge));

        // ACT & ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () ->
                paymentService.processPayment(validBalanceRequest)
        );

        assertEquals("Saldo insuficiente para realizar o pagamento.", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(transactionRepository, never()).save(any(TransactionEntity.class));

        ArgumentCaptor<Charge> chargeCaptor = ArgumentCaptor.forClass(Charge.class);
        verify(chargeRepository, times(1)).save(chargeCaptor.capture());
        assertEquals(ChargeStatus.FAILED, chargeCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("4. Falha: Autorização negada para pagamento CARD")
    void shouldFailPaymentWhenAuthorizationIsDenied() {
        mockSuccessfulUserLookup();
        when(authorizerClient.isApproved()).thenReturn(false);

        when(chargeRepository.findFirstByOriginatorAndRecipientAndAmountAndStatus(any(), any(), any(), eq(ChargeStatus.PENDING)))
                .thenReturn(Optional.of(pendingCharge));

        // ACT & ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () ->
                paymentService.processPayment(validCardRequest)
        );

        assertEquals("Transação negada pelo autorizador externo.", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(transactionRepository, never()).save(any(TransactionEntity.class));

        ArgumentCaptor<Charge> chargeCaptor = ArgumentCaptor.forClass(Charge.class);
        verify(chargeRepository, times(1)).save(chargeCaptor.capture());
        assertEquals(ChargeStatus.FAILED, chargeCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("5. Falha: Originador inativo")
    void shouldFailPaymentWhenOriginatorIsInactive() {
        // ARRANGE
        User inactiveOriginator = User.builder()
                .id(originator.getId()).cpf(originator.getCpf()).fullName(originator.getFullName())
                .balance(originator.getBalance()).email(originator.getEmail()).password(originator.getPassword())
                .ativo(false)
                .createdAt(originator.getCreatedAt()).build();

        when(userRepository.findByCpf(originator.getCpf())).thenReturn(Optional.of(inactiveOriginator));
        when(userRepository.findByCpf(recipient.getCpf())).thenReturn(Optional.of(recipient));

        // ACT & ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () ->
                paymentService.processPayment(validBalanceRequest)
        );

        assertEquals("Um dos usuários está inativo e não pode realizar transações.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    @DisplayName("6. Sucesso: Depósito via CARD")
    void shouldProcessDepositSuccessfully() {
        // ARRANGE
        when(userRepository.findByCpf(recipient.getCpf())).thenReturn(Optional.of(recipient));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(authorizerClient.isApproved()).thenReturn(true);
        mockSuccessfulTransactionSave();

        // ACT
        PaymentResponse response = paymentService.deposit(validDepositRequest);

        // ASSERT
        assertNotNull(response);
        assertEquals(ChargeStatus.SUCCESS, response.status());
        assertEquals(TRANSACTION_AMOUNT, response.amount());

        // Verifica saldos
        assertEquals(new BigDecimal("300.00"), recipient.getBalance()); // 200.00 + 100.00

        // Verifica saves
        verify(authorizerClient, times(1)).isApproved();
        verify(userRepository, times(1)).save(any(User.class));
        verify(transactionRepository, times(1)).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("7. Falha: Depósito negado pelo autorizador")
    void shouldFailDepositWhenAuthorizationIsDenied() {
        // ARRANGE
        when(userRepository.findByCpf(recipient.getCpf())).thenReturn(Optional.of(recipient));
        when(authorizerClient.isApproved()).thenReturn(false);

        // ACT & ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () ->
                paymentService.deposit(validDepositRequest)
        );

        assertEquals("Depósito negado pelo autorizador externo.", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("8. Falha: Destinatário inativo para depósito")
    void shouldFailDepositWhenRecipientIsInactive() {
        // ARRANGE
        User inactiveRecipient = User.builder()
                .id(recipient.getId()).cpf(recipient.getCpf()).fullName(recipient.getFullName())
                .balance(recipient.getBalance()).email(recipient.getEmail()).password(recipient.getPassword())
                .ativo(false)
                .createdAt(recipient.getCreatedAt()).build();

        when(userRepository.findByCpf(recipient.getCpf())).thenReturn(Optional.of(inactiveRecipient));

        // ACT & ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () ->
                paymentService.deposit(validDepositRequest)
        );

        assertEquals("Usuário inativo não pode receber depósitos.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}






