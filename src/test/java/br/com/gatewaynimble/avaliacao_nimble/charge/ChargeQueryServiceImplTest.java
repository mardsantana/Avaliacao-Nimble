package br.com.gatewaynimble.avaliacao_nimble.charge;

import br.com.gatewaynimble.avaliacao_nimble.charge.impl.ChargeQueryServiceImpl;
import br.com.gatewaynimble.avaliacao_nimble.charge.repository.ChargeRepository;
import br.com.gatewaynimble.avaliacao_nimble.charge.records.ChargeResponse;
import br.com.gatewaynimble.avaliacao_nimble.domain.Charge;
import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.exception.ResourceNotFoundException;
import br.com.gatewaynimble.avaliacao_nimble.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus.PENDING;
import static br.com.gatewaynimble.avaliacao_nimble.domain.enums.PaymentMethod.PIX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChargeQueryServiceImplTest {

    @Mock
    private ChargeRepository chargeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChargeQueryServiceImpl chargeQueryService;

    private UUID chargeId;
    private String cpf;
    private User originator;
    private User recipient;
    private Charge charge;

    @BeforeEach
    void setUp() {
        chargeId = UUID.randomUUID();
        cpf = "12345678900";

        originator = User.builder()
                .id(UUID.randomUUID())
                .cpf(cpf)
                .fullName("Originator User")
                .balance(BigDecimal.ZERO)
                .email("ori@test.com")
                .password("test")
                .createdAt(LocalDateTime.now())
                .build();

        recipient = User.builder()
                .id(UUID.randomUUID())
                .cpf("00987654321")
                .fullName("Recipient User")
                .balance(BigDecimal.ZERO)
                .email("rec@test.com")
                .password("test")
                .createdAt(LocalDateTime.now())
                .build();

        charge = Charge.builder()
                .id(chargeId)
                .originator(originator)
                .recipient(recipient)
                .amount(new BigDecimal("100.00"))
                .paymentMethod(PIX)
                .status(PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }


    @Test
    @DisplayName("Deve retornar ChargeResponse ao buscar por ID existente")
    void shouldReturnChargeResponseWhenFindByIdExists() {
        // ARRANGE
        when(chargeRepository.findById(chargeId)).thenReturn(Optional.of(charge));

        // ACT
        ChargeResponse response = chargeQueryService.findById(chargeId);

        // ASSERT
        assertNotNull(response);
        assertEquals(chargeId, response.id());

        verify(chargeRepository, times(1)).findById(chargeId);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar por ID inexistente")
    void shouldThrowExceptionWhenFindByIdNotFound() {
        // ARRANGE
        when(chargeRepository.findById(chargeId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                chargeQueryService.findById(chargeId)
        );

        assertEquals("Cobrança não encontrada", exception.getMessage());

        verify(chargeRepository, times(1)).findById(chargeId);
    }


    @Test
    @DisplayName("Deve retornar lista de ChargeResponse para um usuário existente (Originador/Destinatário)")
    void shouldReturnChargeListWhenFindByUserExists() {
        // ARRANGE
        User user = originator;
        Charge chargeAsRecipient = Charge.builder()
                .id(UUID.randomUUID())
                .originator(recipient)
                .recipient(user)
                .amount(BigDecimal.TEN)
                .status(PENDING)
                .paymentMethod(PIX)
                .createdAt(LocalDateTime.now())
                .build();

        List<Charge> chargesFound = Arrays.asList(charge, chargeAsRecipient);

        when(userRepository.findByCpf(cpf)).thenReturn(Optional.of(user));
        when(chargeRepository.findByOriginatorOrRecipient(user, user)).thenReturn(chargesFound);

        // ACT
        List<ChargeResponse> responses = chargeQueryService.findByUser(cpf);

        // ASSERT
        assertNotNull(responses);
        assertEquals(2, responses.size());

        verify(userRepository, times(1)).findByCpf(cpf);
        verify(chargeRepository, times(1)).findByOriginatorOrRecipient(user, user);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando o CPF não for encontrado")
    void shouldThrowExceptionWhenCpfNotFound() {
        // ARRANGE
        when(userRepository.findByCpf(cpf)).thenReturn(Optional.empty());

        // ACT & ASSERT
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                chargeQueryService.findByUser(cpf)
        );

        assertTrue(exception.getMessage().contains("Usuário não encontrado com CPF"));

        verify(chargeRepository, never()).findByOriginatorOrRecipient(any(), any());
    }
}


