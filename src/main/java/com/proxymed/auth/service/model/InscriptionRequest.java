package com.proxymed.auth.service.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InscriptionRequest(
        @NotBlank(message = "le nom complet est obligatoire")
        @Size(max = 255, message = "le nom complet ne doit pas depasser 255 caracteres")
        String nomComplet,

        @NotBlank(message = "l'email est obligatoire")
        @Email(message = "l'email n'est pas valide")
        @Size(max = 255, message = "l'email ne doit pas depasser 255 caracteres")
        String email,

        @NotBlank(message = "le mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "le mot de passe doit contenir entre 8 et 72 caracteres")
        String motDePasse
) {
    /** Le toString genere par le record afficherait le mot de passe en clair. */
    @Override
    public String toString() {
        return "InscriptionRequest[nomComplet=" + nomComplet + ", email=" + email + ", motDePasse=****]";
    }
}
