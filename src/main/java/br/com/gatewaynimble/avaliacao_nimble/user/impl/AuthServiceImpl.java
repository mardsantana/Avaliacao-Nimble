package br.com.gatewaynimble.avaliacao_nimble.user.impl;

import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.exception.InvalidCredentialsException;
import br.com.gatewaynimble.avaliacao_nimble.security.CustomUserDetails;
import br.com.gatewaynimble.avaliacao_nimble.security.JwtUtil;
import br.com.gatewaynimble.avaliacao_nimble.user.records.LoginRequest;
import br.com.gatewaynimble.avaliacao_nimble.user.repository.UserRepository;
import br.com.gatewaynimble.avaliacao_nimble.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public String login(LoginRequest request) {
        log.info("[start] AuthServiceImpl - login");
        User user = userRepository.findByCpf(request.identifier())
                .or(() -> userRepository.findByEmail(request.identifier()))
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }
        log.info("Verificação de Condições");

        CustomUserDetails userDetails = new CustomUserDetails(user);
        log.info("[finish] AuthServiceImpl - login");
        return jwtUtil.generateToken(userDetails);
    }
}

