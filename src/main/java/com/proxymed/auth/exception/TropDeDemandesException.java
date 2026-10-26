package com.proxymed.auth.exception;

import lombok.Getter;

/**
 * Nouvel envoi de code demande trop tot (429). secondesAAttendre alimente l'en-tete Retry-After.
 */
@Getter
public class TropDeDemandesException extends RuntimeException {

    private final long secondesAAttendre;

    public TropDeDemandesException(long secondesAAttendre) {
        super("Un code vient d'etre envoye. Reessayez dans " + secondesAAttendre + " seconde(s).");
        this.secondesAAttendre = secondesAAttendre;
    }
}
