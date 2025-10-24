package br.com.gatewaynimble.avaliacao_nimble.charge.impl;

import br.com.gatewaynimble.avaliacao_nimble.charge.repository.ChargeRepository;
import br.com.gatewaynimble.avaliacao_nimble.charge.records.ChargeRequest;
import br.com.gatewaynimble.avaliacao_nimble.charge.records.ChargeResponse;
import br.com.gatewaynimble.avaliacao_nimble.charge.service.ChargeService;
import br.com.gatewaynimble.avaliacao_nimble.domain.Charge;
import br.com.gatewaynimble.avaliacao_nimble.domain.enums.ChargeStatus;
import br.com.gatewaynimble.avaliacao_nimble.exception.BusinessException;
import br.com.gatewaynimble.avaliacao_nimble.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeServiceImpl implements ChargeService {

    private final ChargeRepository chargeRepository;
    private final UserRepository userRepository;

    @Override
    public ChargeResponse createCharge(ChargeRequest request) {
        log.info("[start] ChargeServiceImpl - createCharge");

        var originator = userRepository.findByCpf(request.originatorCpf())
                .orElseThrow(() -> new BusinessException("Originador não encontrado"));

        var recipient = userRepository.findByCpf(request.recipientCpf())
                .orElseThrow(() -> new BusinessException("Destinatário não encontrado"));

        if (originator.getId().equals(recipient.getId())) {
            throw new BusinessException("O originador e o destinatário não podem ser o mesmo usuário");
        }

        var charge = Charge.builder()
                .originator(originator)
                .recipient(recipient)
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .status(ChargeStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        chargeRepository.save(charge);
        log.info("[finish] ChargeServiceImpl - createCharge");

        return ChargeResponse.from(charge);
    }
}