package com.proxymed.auth.configs.security;

import java.util.List;
import java.util.UUID;

/**
 * Identite portee par le badge, placee dans le SecurityContext par AuthTokenFilter.
 * Construite uniquement a partir des claims : aucun acces base n'est fait pour authentifier
 * une requete (les Fiches 1 et 2 feront de meme).
 */
public record UtilisateurConnecte(
        UUID id,
        String email,
        List<String> roles
) {
}
