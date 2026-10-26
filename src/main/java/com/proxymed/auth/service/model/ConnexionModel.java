package com.proxymed.auth.service.model;

import lombok.Builder;

/**
 * Resultat d'une connexion reussie : le badge et le compte auquel il appartient.
 */
@Builder
public record ConnexionModel(
        String accessToken,
        long expireDansSecondes,
        UtilisateurModel utilisateur
) {
}
