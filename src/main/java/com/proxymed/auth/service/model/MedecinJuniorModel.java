package com.proxymed.auth.service.model;

import lombok.Builder;

import java.util.UUID;

/**
 * Vue minimale d'un medecin junior, pour qu'un MEDECIN_SENIOR puisse le choisir.
 * Ne porte volontairement ni email, ni role, ni mot de passe.
 */
@Builder(toBuilder = true)
public record MedecinJuniorModel(
        UUID id,
        String nomComplet
) {
}
