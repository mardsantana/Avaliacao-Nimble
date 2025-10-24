package br.com.gatewaynimble.avaliacao_nimble.user;

import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.exception.ResourceNotFoundException;
import br.com.gatewaynimble.avaliacao_nimble.user.impl.UserServiceImpl;
import br.com.gatewaynimble.avaliacao_nimble.user.records.UserRequest;
import br.com.gatewaynimble.avaliacao_nimble.user.records.UserResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRequest validUserRequest;
    private final String TEST_CPF = "12345678900";
    private final String TEST_EMAIL = "newuser@test.com";
    private final String TEST_PASSWORD = "securepassword";
    private final String ENCODED_PASSWORD = "$2a$10$encodedpasswordhash";

    @BeforeEach
    void setUp() {
        validUserRequest = new UserRequest(
                TEST_CPF,
                TEST_EMAIL,
                "João da Silva",
                TEST_PASSWORD
        );

        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(UUID.randomUUID())
                    .cpf(user.getCpf())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .password(user.getPassword())
                    .balance(user.getBalance())
                    .ativo(user.isAtivo())
                    .createdAt(user.getCreatedAt())
                    .version(0L)
                    .build();
        });
    }


    @Test
    @DisplayName("1. Sucesso: Deve registrar um novo usuário com saldo ZERO e senha criptografada")
    void shouldRegisterNewUserSuccessfully() {
        // ARRANGE
        when(userRepository.findByCpf(TEST_CPF)).thenReturn(Optional.empty());

        // ACT
        UserResponse response = userService.register(validUserRequest);

        // ASSERT
        assertNotNull(response);
        assertEquals(TEST_CPF, response.cpf());
        assertEquals(BigDecimal.ZERO, response.balance(), "O saldo inicial deve ser ZERO.");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
        verify(userRepository, times(1)).save(userCaptor.capture());

        // ASSERT
        User savedUser = userCaptor.getValue();
        assertEquals(ENCODED_PASSWORD, savedUser.getPassword(), "A senha deve ser criptografada.");
        assertTrue(savedUser.isAtivo(), "O status 'ativo' deve ser verdadeiro por padrão.");
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    @DisplayName("2. Falha: Deve lançar exceção se o CPF já estiver cadastrado")
    void shouldThrowExceptionWhenCpfAlreadyExists() {
        // ARRANGE
        User existingUser = User.builder().cpf(TEST_CPF).build();
        when(userRepository.findByCpf(TEST_CPF)).thenReturn(Optional.of(existingUser));

        // ACT & ASSERT
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.register(validUserRequest)
        );

        assertEquals("CPF já cadastrado", exception.getMessage());

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }


    @Test
    @DisplayName("3. Sucesso: Deve encontrar e retornar o usuário pelo CPF")
    void shouldFindUserByCpfSuccessfully() {
        // ARRANGE
        User foundUser = User.builder()
                .id(UUID.randomUUID())
                .cpf(TEST_CPF)
                .email(TEST_EMAIL)
                .fullName("Encontrado")
                .balance(new BigDecimal("500.00"))
                .ativo(true)
                .build();

        when(userRepository.findByCpf(TEST_CPF)).thenReturn(Optional.of(foundUser));

        // ACT
        UserResponse response = userService.findByCpf(TEST_CPF);

        // ASSERT
        assertNotNull(response);
        assertEquals(TEST_CPF, response.cpf());
        assertEquals("Encontrado", response.fullName());
        assertEquals(new BigDecimal("500.00"), response.balance());

        verify(userRepository, times(1)).findByCpf(TEST_CPF);
    }

    @Test
    @DisplayName("4. Falha: Deve lançar exceção se o usuário não for encontrado pelo CPF")
    void shouldThrowExceptionWhenUserIsNotFoundByCpf() {
        // ARRANGE
        String nonExistentCpf = "99999999999";
        when(userRepository.findByCpf(nonExistentCpf)).thenReturn(Optional.empty());

        // ACT & ASSERT
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                userService.findByCpf(nonExistentCpf)
        );

        assertEquals("Usuário não encontrado com CPF: 99999999999", exception.getMessage());

        verify(userRepository, times(1)).findByCpf(nonExistentCpf);
    }
}


