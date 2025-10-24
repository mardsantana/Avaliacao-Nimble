package br.com.gatewaynimble.avaliacao_nimble.charge.impl;

import br.com.gatewaynimble.avaliacao_nimble.charge.repository.ChargeRepository;
import br.com.gatewaynimble.avaliacao_nimble.charge.records.ChargeResponse;
import br.com.gatewaynimble.avaliacao_nimble.charge.service.ChargeQueryService;
import br.com.gatewaynimble.avaliacao_nimble.exception.ResourceNotFoundException;
import br.com.gatewaynimble.avaliacao_nimble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeQueryServiceImpl implements ChargeQueryService {

    private final ChargeRepository chargeRepository;
    private final UserRepository userRepository;

    @Override
    public ChargeResponse findById(UUID chargeId) {
        log.info("[start] ChargeQueryServiceImpl - findById");
        var charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cobrança não encontrada"));
        log.info("[finish] ChargeQueryServiceImpl - findById");
        return ChargeResponse.from(charge);
    }

    @Override
    public List<ChargeResponse> findByUser(String cpf) {
        log.info("[start] ChargeQueryServiceImpl - findByUser");
        var user = userRepository.findByCpf(cpf)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com CPF: " + cpf));

        return chargeRepository.findByOriginatorOrRecipient(user, user)
                .stream()
                .map(ChargeResponse::from)
                .toList();
    }
}
