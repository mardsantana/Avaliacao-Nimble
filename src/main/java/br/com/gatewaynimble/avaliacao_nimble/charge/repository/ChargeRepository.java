package br.com.gatewaynimble.avaliacao_nimble.charge.repository;

import br.com.gatewaynimble.avaliacao_nimble.domain.Charge;
import br.com.gatewaynimble.avaliacao_nimble.domain.User;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ChargeRepository extends JpaRepository<Charge, UUID> {

    Collection<Charge> findByOriginatorOrRecipient(User user, User user1);
    Optional<Charge> findFirstByOriginatorAndRecipientAndAmountAndStatus(
            User originator, User recipient, BigDecimal amount, ChargeStatus pending);
}
