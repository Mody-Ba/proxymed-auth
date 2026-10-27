package com.proxymed.auth.service.model;

import lombok.Builder;

@Builder
public record ChangementMotDePasseModel(
        String ancienMotDePasse,
        String nouveauMotDePasse
) {
    /** Le toString genere par le record afficherait les mots de passe en clair. */
    @Override
    public String toString() {
        return "ChangementMotDePasseModel[ancienMotDePasse=****, nouveauMotDePasse=****]";
    }
}
