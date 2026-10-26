package com.proxymed.auth.enums;

/**
 * Roles PROXYMED. Le nom de la constante est la valeur stockee en base (colonne role)
 * et celle transportee dans le claim "roles" du badge.
 */
public enum Role {
    /** Patient ou proche du patient. */
    UTILISATEUR,
    MEDECIN_JUNIOR,
    MEDECIN_SENIOR,
    ADMIN
}
