package com.proxymed.auth.exception;

/**
 * Email inconnu ou mot de passe faux. Volontairement un seul cas : le message renvoye
 * au client ne doit pas dire lequel des deux est faux.
 */
public class IdentifiantsInvalidesException extends RuntimeException {

    public IdentifiantsInvalidesException() {
        super("Email ou mot de passe incorrect");
    }
}
