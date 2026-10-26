package com.proxymed.auth.service.model;

import lombok.Builder;

import java.time.Instant;

@Builder
public record InscriptionEnAttenteResponse(
        String message,
        String email,
        Instant codeExpireLe,
        Instant renvoiPossibleLe
) {
}
