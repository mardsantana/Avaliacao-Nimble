package br.com.gatewaynimble.avaliacao_nimble.user;

import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.exception.InvalidCredentialsException;
import br.com.gatewaynimble.avaliacao_nimble.security.CustomUserDetails;
import br.com.gatewaynimble.avaliacao_nimble.security.JwtUtil;
import br.com.gatewaynimble.avaliacao_nimble.user.impl.AuthServiceImpl;
import br.com.gatewaynimble.avaliacao_nimble.user.records.LoginRequest;
import br.com.gatewaynimble.avaliacao_nimble.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private final String VALID_CPF = "12345678900";
    private final String VALID_EMAIL = "user@test.com";
    private final String VALID_PASSWORD = "password123";
    private final String ENCODED_PASSWORD = "$2a$10$encodedpasswordhash";
    private final String GENERATED_TOKEN = "mocked.jwt.token";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .cpf(VALID_CPF)
                .email(VALID_EMAIL)
                .password(ENCODED_PASSWORD)
                .fullName("Test User")
                .ativo(true)
                .build();
    }


    private void mockSuccessfulPasswordMatch() {
        when(passwordEncoder.matches(eq(VALID_PASSWORD), eq(ENCODED_PASSWORD))).thenReturn(true);
    }

    private void mockTokenGeneration() {
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn(GENERATED_TOKEN);
    }


    @Test
    @DisplayName("1. Sucesso: Login deve funcionar ao usar CPF válido")
    void shouldLoginSuccessfullyWithCpf() {
        // ARRANGE
        when(userRepository.findByCpf(VALID_CPF)).thenReturn(Optional.of(testUser));

        mockSuccessfulPasswordMatch();
        mockTokenGeneration();

        LoginRequest request = new LoginRequest(VALID_CPF, VALID_PASSWORD);

        // ACT
        String token = authService.login(request);

        // ASSERT
        assertNotNull(token);
        assertEquals(GENERATED_TOKEN, token);

        verify(userRepository, times(1)).findByCpf(VALID_CPF);
        verify(userRepository, never()).findByEmail(any());
        verify(passwordEncoder, times(1)).matches(VALID_PASSWORD, ENCODED_PASSWORD);
        verify(jwtUtil, times(1)).generateToken(any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("2. Sucesso: Login deve funcionar ao usar Email válido")
    void shouldLoginSuccessfullyWithEmail() {
        // ARRANGE
        when(userRepository.findByCpf(VALID_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(testUser));

        mockSuccessfulPasswordMatch();
        mockTokenGeneration();

        LoginRequest request = new LoginRequest(VALID_EMAIL, VALID_PASSWORD);

        // ACT
        String token = authService.login(request);

        // ASSERT
        assertNotNull(token);
        assertEquals(GENERATED_TOKEN, token);

        verify(userRepository, times(1)).findByCpf(VALID_EMAIL);
        verify(userRepository, times(1)).findByEmail(VALID_EMAIL);
        verify(passwordEncoder, times(1)).matches(VALID_PASSWORD, ENCODED_PASSWORD);
        verify(jwtUtil, times(1)).generateToken(any(CustomUserDetails.class));
    }


    @Test
    @DisplayName("3. Falha: Deve lançar exceção se o usuário não for encontrado")
    void shouldThrowExceptionWhenUserIsNotFound() {
        // ARRANGE
        when(userRepository.findByCpf(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("nonexistent", VALID_PASSWORD);

        // ACT & ASSERT
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () ->
                authService.login(request)
        );

        assertEquals("Credenciais inválidas", exception.getMessage());

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("4. Falha: Deve lançar exceção se a senha for inválida")
    void shouldThrowExceptionWhenPasswordIsInvalid() {
        // ARRANGE
        when(userRepository.findByCpf(VALID_CPF)).thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches(eq("wrong_password"), eq(ENCODED_PASSWORD))).thenReturn(false);

        LoginRequest request = new LoginRequest(VALID_CPF, "wrong_password");

        // ACT & ASSERT
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () ->
                authService.login(request)
        );

        assertEquals("Credenciais inválidas", exception.getMessage());

        verify(passwordEncoder, times(1)).matches("wrong_password", ENCODED_PASSWORD);
        verify(jwtUtil, never()).generateToken(any());
    }
}

