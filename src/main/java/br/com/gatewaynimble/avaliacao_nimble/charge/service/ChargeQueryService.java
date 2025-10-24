package br.com.gatewaynimble.avaliacao_nimble.charge.service;

import br.com.gatewaynimble.avaliacao_nimble.charge.records.ChargeResponse;

import java.util.List;
import java.util.UUID;

public interface ChargeQueryService {
    ChargeResponse findById(UUID id);
    List<ChargeResponse> findByUser(String cpf);
}
