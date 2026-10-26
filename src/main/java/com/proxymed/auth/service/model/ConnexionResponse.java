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
        Role role
) {
}
