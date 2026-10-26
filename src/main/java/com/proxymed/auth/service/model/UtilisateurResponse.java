package com.proxymed.auth.service.model;

import com.proxymed.auth.enums.Role;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record UtilisateurResponse(
        UUID id,
        String email,
        String nomComplet,
        Role role,
        boolean actif,
        Instant dateCreation
) {
}
