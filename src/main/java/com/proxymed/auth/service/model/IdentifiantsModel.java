package com.proxymed.auth.service.model;

import lombok.Builder;

/**
 * Identifiants saisis a la connexion. Le mot de passe en clair ne vit que le temps de la
 * verification BCrypt dans AuthServiceImpl.
 */
@Builder
public record IdentifiantsModel(
        String email,
        String motDePasse
) {
    /** Le toString genere par le record afficherait le mot de passe en clair. */
    @Override
    public String toString() {
        return "IdentifiantsModel[email=" + email + ", motDePasse=****]";
    }
}
