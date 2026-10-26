package com.proxymed.auth.exception;

/**
 * Identifiants corrects mais compte desactive.
 */
public class CompteInactifException extends RuntimeException {

    public CompteInactifException() {
        super("Ce compte est desactive. Contactez un administrateur.");
    }
}
