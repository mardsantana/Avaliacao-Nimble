package br.com.gatewaynimble.avaliacao_nimble.authorizer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizerResponse {
    private String message;
}