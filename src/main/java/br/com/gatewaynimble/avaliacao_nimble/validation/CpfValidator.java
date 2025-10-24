package br.com.gatewaynimble.avaliacao_nimble.validation;

import jakarta.validation.ConstraintValidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null) return false;

        String onlyDigits = cpf.replaceAll("\\D", "");
        if (onlyDigits.length() != 11) return false;

        // Rejeita CPFs com todos os dígitos iguais
        if (onlyDigits.chars().distinct().count() == 1) return false;

        try {
            int[] nums = new int[11];
            for (int i = 0; i < 11; i++) {
                nums[i] = Integer.parseInt(onlyDigits.substring(i, i + 1));
            }

            // primeiro dígito verificador
            int sum = 0;
            for (int i = 0; i < 9; i++) sum += nums[i] * (10 - i);
            int r = sum % 11;
            int dig10 = (r < 2) ? 0 : 11 - r;
            if (nums[9] != dig10) return false;

            // segundo dígito verificador
            sum = 0;
            for (int i = 0; i < 10; i++) sum += nums[i] * (11 - i);
            r = sum % 11;
            int dig11 = (r < 2) ? 0 : 11 - r;
            return nums[10] == dig11;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
