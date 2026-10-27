package com.proxymed.auth.service.email;

import java.time.Duration;

/**
 * Envoi des emails de PROXYMED. Interface pour pouvoir changer de fournisseur (SMTP aujourd'hui,
 * API d'un service d'emailing demain) sans toucher aux services metier.
 */
public interface EmailService {

    /**
     * Envoie le code de verification d'une inscription.
     *
     * @throws com.proxymed.auth.exception.EnvoiEmailException si l'envoi echoue
     */
    void envoyerCodeInscription(String destinataire, String nomComplet, String code, Duration validite);

    /**
     * Envoie le mot de passe temporaire d'un compte cree ou reinitialise par un ADMIN.
     *
     * @throws com.proxymed.auth.exception.EnvoiEmailException si l'envoi echoue
     */
    void envoyerMotDePasseTemporaire(String destinataire, String nomComplet, String motDePasseTemporaire);
}
