package br.com.gatewaynimble.avaliacao_nimble.user.impl;

import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.exception.ResourceNotFoundException;
import br.com.gatewaynimble.avaliacao_nimble.user.records.UserRequest;
import br.com.gatewaynimble.avaliacao_nimble.user.records.UserResponse;
import br.com.gatewaynimble.avaliacao_nimble.user.repository.UserRepository;
import br.com.gatewaynimble.avaliacao_nimble.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse register(UserRequest request) {
        log.info("[start] UserServiceImpl - register");
        if (userRepository.findByCpf(request.cpf()).isPresent()) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }

        User user = User.builder()
                .cpf(request.cpf())
                .email(request.email())
                .fullName(request.fullName())
                .password(passwordEncoder.encode(request.password()))
                .balance(BigDecimal.ZERO)
                .ativo(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("[finish] UserServiceImpl - register");
        return new UserResponse(
                user.getId(),
                user.getCpf(),
                user.getEmail(),
                user.getFullName(),
                user.getBalance());
    }

    @Override
    public UserResponse findByCpf(String cpf) {
        log.info("[start] UserServiceImpl - findByCpf");
        User user = userRepository.findByCpf(cpf)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com CPF: " + cpf));

        log.info("[finish] UserServiceImpl - findByCpf");
        return new UserResponse(
                user.getId(),
                user.getCpf(),
                user.getEmail(),
                user.getFullName(),
                user.getBalance());
    }
}
