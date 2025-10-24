package br.com.gatewaynimble.avaliacao_nimble.authorizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizerClient {

    private final WebClient.Builder webClientBuilder;

    private static final String AUTHORIZE_URL = "https://zsy6tx7aql.execute-api.sa-east-1.amazonaws.com/authorizer";

    @Value("${app.authorizer.mock-mode:false}")
    private boolean mockMode;

    public boolean isApproved() {
        if (mockMode) {
            log.warn("Modo simulação ativo: simulando autorização aleatória.");
            return Math.random() > 0.3; // 70% de chance de “Autorizado”
        }

        try {
            AuthorizerResponse response = webClientBuilder.build()
                    .get()
                    .uri(AUTHORIZE_URL)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            resp -> Mono.error(new RuntimeException("Erro HTTP do autorizador: " + resp.statusCode()))
                    )
                    .bodyToMono(AuthorizerResponse.class)
                    .block();

            if (response == null) {
                log.error("Resposta nula do autorizador externo.");
                return false;
            }

            log.info("Resposta do autorizador: {}", response.getMessage());
            return "Autorizado".equalsIgnoreCase(response.getMessage());
        } catch (Exception e) {
            log.error("Erro ao consultar autorizador externo: {}", e.getMessage());
            return false;
        }
    }
}