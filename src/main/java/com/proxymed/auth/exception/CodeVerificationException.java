package com.proxymed.auth.exception;

/**
 * Code de verification refuse : faux, expire, ou nombre maximal d'essais atteint (400).
 */
public class CodeVerificationException extends RuntimeException {

    public CodeVerificationException(String message) {
        super(message);
    }
}
