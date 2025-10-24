package br.com.gatewaynimble.avaliacao_nimble.payment.repository;

import br.com.gatewaynimble.avaliacao_nimble.domain.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
}
