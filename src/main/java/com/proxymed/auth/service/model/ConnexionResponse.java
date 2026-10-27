package com.proxymed.auth.service.model;

import com.proxymed.auth.enums.Role;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ConnexionResponse(
        String accessToken,
        String type,
        long expireDansSecondes,
        UUID utilisateurId,
        String email,
        Role role,
        /** true : le mot de passe a ete genere par un ADMIN et doit etre change (/api/auth/changer-mot-de-passe). */
        boolean motDePasseTemporaire
) {
}
