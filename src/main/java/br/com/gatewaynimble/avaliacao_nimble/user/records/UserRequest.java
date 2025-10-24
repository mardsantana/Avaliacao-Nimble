package br.com.gatewaynimble.avaliacao_nimble.user.records;

import br.com.gatewaynimble.avaliacao_nimble.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @Cpf
        String cpf,

        @Email(message = "E-mail inválido")
        @NotBlank(message = "E-mail é obrigatório")
        String email,

        @NotBlank(message = "Nome completo é obrigatório")
        String fullName,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String password
) {}
