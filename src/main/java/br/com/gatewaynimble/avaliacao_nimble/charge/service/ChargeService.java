package br.com.gatewaynimble.avaliacao_nimble.charge.service;

import br.com.gatewaynimble.avaliacao_nimble.charge.records.ChargeRequest;
import br.com.gatewaynimble.avaliacao_nimble.charge.records.ChargeResponse;

public interface ChargeService {
    ChargeResponse createCharge(ChargeRequest request);
}
