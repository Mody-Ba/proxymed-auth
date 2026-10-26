package com.proxymed.auth.service.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerificationRequest(
        @NotBlank(message = "l'email est obligatoire")
        @Email(message = "l'email n'est pas valide")
        @Size(max = 255, message = "l'email ne doit pas depasser 255 caracteres")
        String email,

        @NotBlank(message = "le code est obligatoire")
        @Pattern(regexp = "\\d{6}", message = "le code doit contenir exactement 6 chiffres")
        String code
) {
    @Override
    public String toString() {
        return "VerificationRequest[email=" + email + ", code=****]";
    }
}
