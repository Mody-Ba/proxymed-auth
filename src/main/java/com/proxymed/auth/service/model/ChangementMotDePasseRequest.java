package com.proxymed.auth.service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangementMotDePasseRequest(
        @NotBlank(message = "l'ancien mot de passe est obligatoire")
        @Size(max = 72, message = "l'ancien mot de passe ne doit pas depasser 72 caracteres")
        String ancienMotDePasse,

        @NotBlank(message = "le nouveau mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "le nouveau mot de passe doit contenir entre 8 et 72 caracteres")
        String nouveauMotDePasse
) {
    /** Le toString genere par le record afficherait les mots de passe en clair. */
    @Override
    public String toString() {
        return "ChangementMotDePasseRequest[ancienMotDePasse=****, nouveauMotDePasse=****]";
    }
}
