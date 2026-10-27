package com.proxymed.auth.service.model;

import jakarta.validation.constraints.NotNull;

public record ActivationRequest(
        @NotNull(message = "actif est obligatoire (true ou false)")
        Boolean actif
) {
}
