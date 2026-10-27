package com.proxymed.auth.service;

import com.proxymed.auth.enums.Role;
import com.proxymed.auth.service.model.CreationCompteModel;
import com.proxymed.auth.service.model.UtilisateurModel;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Gestion des comptes par un ADMIN. Ne manipule que des comptes : aucune donnee medicale.
 */
public interface AdminUtilisateurService {

    /**
     * Cree un compte MEDECIN_JUNIOR ou MEDECIN_SENIOR avec un mot de passe temporaire envoye par email.
     *
     * @throws com.proxymed.auth.exception.RegleGestionException role autre que MEDECIN_JUNIOR / MEDECIN_SENIOR
     * @throws com.proxymed.auth.exception.CompteExistantException email deja utilise
     * @throws com.proxymed.auth.exception.EnvoiEmailException email non envoye (rien n'est enregistre)
     */
    UtilisateurModel creerCompteMedecin(CreationCompteModel creation);

    /** role null : tous les comptes. */
    Page<UtilisateurModel> lister(Role role, int page, int taille);

    UtilisateurModel findById(UUID id);

    /**
     * @param adminConnecteId l'ADMIN qui fait la demande : il ne peut pas se desactiver lui-meme
     */
    UtilisateurModel changerActivation(UUID id, boolean actif, UUID adminConnecteId);

    /**
     * Genere un nouveau mot de passe temporaire et l'envoie par email.
     *
     * @throws com.proxymed.auth.exception.EnvoiEmailException email non envoye (l'ancien mot de passe reste valable)
     */
    UtilisateurModel reinitialiserMotDePasse(UUID id);
}
