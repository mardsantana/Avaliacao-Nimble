package br.com.gatewaynimble.avaliacao_nimble.user.repository;

import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByCpf(String cpf);
    Optional<User> findByEmail(String email);
}
