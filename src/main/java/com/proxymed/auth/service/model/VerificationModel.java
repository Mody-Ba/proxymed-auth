package com.proxymed.auth.service.model;

import lombok.Builder;

@Builder
public record VerificationModel(
        String email,
        String code
) {
    @Override
    public String toString() {
        return "VerificationModel[email=" + email + ", code=****]";
    }
}
