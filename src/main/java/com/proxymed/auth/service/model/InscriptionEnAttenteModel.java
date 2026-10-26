package com.proxymed.auth.service.model;

import lombok.Builder;

import java.time.Instant;

/**
 * Etat d'une inscription en attente renvoye au client. Ne porte ni le code ni aucun hash.
 */
@Builder
public record InscriptionEnAttenteModel(
        String email,
        Instant codeExpireLe,
        Instant renvoiPossibleLe
) {
}
