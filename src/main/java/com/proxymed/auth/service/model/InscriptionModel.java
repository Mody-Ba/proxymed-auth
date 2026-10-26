package com.proxymed.auth.service.model;

import lombok.Builder;

/**
 * Donnees saisies a l'inscription. Le mot de passe en clair ne vit que le temps du hachage
 * BCrypt dans InscriptionServiceImpl.
 */
@Builder
public record InscriptionModel(
        String nomComplet,
        String email,
        String motDePasse
) {
    /** Le toString genere par le record afficherait le mot de passe en clair. */
    @Override
    public String toString() {
        return "InscriptionModel[nomComplet=" + nomComplet + ", email=" + email + ", motDePasse=****]";
    }
}
