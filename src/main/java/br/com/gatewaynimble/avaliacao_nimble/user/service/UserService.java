package br.com.gatewaynimble.avaliacao_nimble.user.service;

import br.com.gatewaynimble.avaliacao_nimble.user.records.UserRequest;
import br.com.gatewaynimble.avaliacao_nimble.user.records.UserResponse;

public interface UserService {
    UserResponse register(UserRequest request);
    UserResponse findByCpf(String cpf);
}
