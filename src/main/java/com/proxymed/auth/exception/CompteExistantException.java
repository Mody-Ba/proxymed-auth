package com.proxymed.auth.exception;

public class CompteExistantException extends RuntimeException {

    public CompteExistantException() {
        super("Un compte existe deja avec cet email");
    }
}
