package com.proxymed.auth.service.model;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record MedecinJuniorResponse(
        UUID id,
        String nomComplet
) {
}
