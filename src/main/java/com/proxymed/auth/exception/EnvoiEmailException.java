package com.proxymed.auth.exception;

/**
 * Le serveur d'email a refuse ou n'a pas pu envoyer le message (503).
 */
public class EnvoiEmailException extends RuntimeException {

    public EnvoiEmailException(Throwable cause) {
        super("L'email n'a pas pu etre envoye. Reessayez plus tard.", cause);
    }
}
