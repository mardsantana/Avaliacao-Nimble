package br.com.gatewaynimble.avaliacao_nimble.charge;

import br.com.gatewaynimble.avaliacao_nimble.charge.impl.ChargeServiceImpl;
import br.com.gatewaynimble.avaliacao_nimble.charge.repository.ChargeRepository;
import br.com.gatewaynimble.avaliacao_nimble.charge.records.ChargeRequest;
import br.com.gatewaynimble.avaliacao_nimble.charge.records.ChargeResponse;
import br.com.gatewaynimble.avaliacao_nimble.domain.Charge;
import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus;
import br.com.gatewaynimble.avaliacao_nimble.exception.BusinessException;
import br.com.gatewaynimble.avaliacao_nimble.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static br.com.gatewaynimble.avaliacao_nimble.domain.enums.PaymentMethod.PIX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChargeServiceImplTest {

    @Mock
    private ChargeRepository chargeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChargeServiceImpl chargeService;

    private User originator;
    private User recipient;
    private ChargeRequest validRequest;

    @BeforeEach
    void setUp() {
        originator = User.builder()
                .id(UUID.randomUUID())
                .cpf("11122233344")
                .fullName("Originator")
                .balance(new BigDecimal("500.00"))
                .email("ori@test.com")
                .password("pass")
                .createdAt(LocalDateTime.now())
                .build();

        recipient = User.builder()
                .id(UUID.randomUUID())
                .cpf("55566677788")
                .fullName("Recipient")
                .balance(new BigDecimal("100.00"))
                .email("rec@test.com")
                .password("pass")
                .createdAt(LocalDateTime.now())
                .build();

        validRequest = new ChargeRequest(
                originator.getCpf(),
                recipient.getCpf(),
                new BigDecimal("50.00"),
                PIX
        );
    }


    @Test
    @DisplayName("Deve criar e salvar uma cobrança com sucesso e retornar ChargeResponse")
    void shouldCreateAndSaveChargeSuccessfully() {
        when(userRepository.findByCpf(validRequest.originatorCpf())).thenReturn(Optional.of(originator));
        when(userRepository.findByCpf(validRequest.recipientCpf())).thenReturn(Optional.of(recipient));

        ArgumentCaptor<Charge> chargeCaptor = ArgumentCaptor.forClass(Charge.class);

        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> {
            Charge capturedCharge = invocation.getArgument(0);
            if (capturedCharge.getId() == null) {
                capturedCharge = Charge.builder()
                        .id(UUID.randomUUID())
                        .originator(capturedCharge.getOriginator())
                        .recipient(capturedCharge.getRecipient())
                        .amount(capturedCharge.getAmount())
                        .paymentMethod(capturedCharge.getPaymentMethod())
                        .status(capturedCharge.getStatus())
                        .createdAt(capturedCharge.getCreatedAt())
                        .build();
            }
            return capturedCharge;
        });

        // ACT
        ChargeResponse response = chargeService.createCharge(validRequest);

        // ASSERT:
        assertNotNull(response);
        assertEquals(validRequest.amount(), response.amount());

        assertEquals(ChargeStatus.PENDING, response.status());

        verify(chargeRepository, times(1)).save(chargeCaptor.capture());

        Charge savedCharge = chargeCaptor.getValue();
        assertEquals(originator.getId(), savedCharge.getOriginator().getId());
        assertEquals(recipient.getId(), savedCharge.getRecipient().getId());
        assertEquals(validRequest.amount(), savedCharge.getAmount());
    }

    @Test
    @DisplayName("Deve lançar BusinessException se o Originador não for encontrado")
    void shouldThrowExceptionWhenOriginatorNotFound() {
        // ARRANGE
        when(userRepository.findByCpf(validRequest.originatorCpf())).thenReturn(Optional.empty());

        // ACT & ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () ->
                chargeService.createCharge(validRequest)
        );

        assertEquals("Originador não encontrado", exception.getMessage());

        verify(chargeRepository, never()).save(any(Charge.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException se o Destinatário não for encontrado")
    void shouldThrowExceptionWhenRecipientNotFound() {
        // ARRANGE
        when(userRepository.findByCpf(validRequest.originatorCpf())).thenReturn(Optional.of(originator));
        when(userRepository.findByCpf(validRequest.recipientCpf())).thenReturn(Optional.empty());

        // ACT & ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () ->
                chargeService.createCharge(validRequest)
        );

        assertEquals("Destinatário não encontrado", exception.getMessage());

        verify(chargeRepository, never()).save(any(Charge.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException se Originador e Destinatário forem o mesmo usuário")
    void shouldThrowExceptionWhenOriginatorIsSameAsRecipient() {
        // ARRANGE
        String sameCpf = "99999999999";

        User sameUser = User.builder()
                .id(UUID.randomUUID())
                .cpf(sameCpf)
                .fullName("Self User")
                .balance(BigDecimal.ONE)
                .email("self@test.com")
                .password("pass")
                .createdAt(LocalDateTime.now())
                .build();

        ChargeRequest sameUserRequest = new ChargeRequest(
                sameCpf, sameCpf, new BigDecimal("10.00"), PIX
        );

        when(userRepository.findByCpf(sameCpf)).thenReturn(Optional.of(sameUser));

        // ACT & ASSERT
        BusinessException exception = assertThrows(BusinessException.class, () ->
                chargeService.createCharge(sameUserRequest)
        );

        assertEquals("O originador e o destinatário não podem ser o mesmo usuário", exception.getMessage());

        verify(chargeRepository, never()).save(any(Charge.class));
    }
}

