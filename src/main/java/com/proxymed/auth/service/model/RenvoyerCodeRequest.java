package com.proxymed.auth.service.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenvoyerCodeRequest(
        @NotBlank(message = "l'email est obligatoire")
        @Email(message = "l'email n'est pas valide")
        @Size(max = 255, message = "l'email ne doit pas depasser 255 caracteres")
        String email
) {
}
