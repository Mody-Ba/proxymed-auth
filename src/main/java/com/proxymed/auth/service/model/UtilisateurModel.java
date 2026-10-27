package com.proxymed.auth.service.model;

import com.proxymed.auth.enums.Role;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (UtilisateurResponse), ni l'entite JPA.
 * Ne porte volontairement pas le mot de passe (ni en clair ni chiffre).
 */
@Builder(toBuilder = true)
public record UtilisateurModel(
        UUID id,
        String email,
        String nomComplet,
        Role role,
        boolean actif,
        boolean motDePasseTemporaire,
        Instant dateCreation
) {
}
