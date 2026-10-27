package com.proxymed.auth.service.model;

import com.proxymed.auth.enums.Role;
import lombok.Builder;

@Builder
public record CreationCompteModel(
        String nomComplet,
        String email,
        Role role
) {
}
