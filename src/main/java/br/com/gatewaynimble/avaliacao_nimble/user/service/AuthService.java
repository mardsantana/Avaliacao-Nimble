package br.com.gatewaynimble.avaliacao_nimble.user.service;

import br.com.gatewaynimble.avaliacao_nimble.user.records.LoginRequest;

public interface AuthService {
    String login(LoginRequest request);
}
