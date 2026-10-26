package com.proxymed.auth.service.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConnexionRequest(
        @NotBlank(message = "l'email est obligatoire")
        @Email(message = "l'email n'est pas valide")
        @Size(max = 255, message = "l'email ne doit pas depasser 255 caracteres")
        String email,

        @NotBlank(message = "le mot de passe est obligatoire")
        @Size(max = 72, message = "le mot de passe ne doit pas depasser 72 caracteres")
        String motDePasse
) {
    /** Le toString genere par le record afficherait le mot de passe en clair. */
    @Override
    public String toString() {
        return "ConnexionRequest[email=" + email + ", motDePasse=****]";
    }
}
