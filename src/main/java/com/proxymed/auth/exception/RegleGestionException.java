package com.proxymed.auth.exception;

/**
 * Violation d'une regle de gestion (400) : role non autorise a la creation, desactivation de son
 * propre compte, ancien mot de passe incorrect, etc.
 */
public class RegleGestionException extends RuntimeException {

    public RegleGestionException(String message) {
        super(message);
    }
}
