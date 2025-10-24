package br.com.gatewaynimble.avaliacao_nimble.security;

import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String cpf) throws UsernameNotFoundException {
        User user = userRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com CPF: " + cpf));
        return new CustomUserDetails(user);
    }
}