package com.proxymed.auth.service;

import com.proxymed.auth.service.model.InscriptionEnAttenteModel;
import com.proxymed.auth.service.model.InscriptionModel;
import com.proxymed.auth.service.model.UtilisateurModel;
import com.proxymed.auth.service.model.VerificationModel;

public interface InscriptionService {

    /**
     * Enregistre (ou remplace) une inscription en attente et envoie un code par email.
     * Aucun compte n'est cree a ce stade.
     *
     * @throws com.proxymed.auth.exception.CompteExistantException un compte existe deja avec cet email
     * @throws com.proxymed.auth.exception.TropDeDemandesException un code a ete envoye il y a moins de 60 secondes
     */
    InscriptionEnAttenteModel inscrire(InscriptionModel inscription);

    /**
     * Valide le code et cree le compte UTILISATEUR.
     *
     * @throws com.proxymed.auth.exception.CodeVerificationException code faux, expire ou trop d'essais
     */
    UtilisateurModel verifier(VerificationModel verification);

    /**
     * Genere et envoie un nouveau code pour une inscription en attente.
     *
     * @throws com.proxymed.auth.exception.TropDeDemandesException un code a ete envoye il y a moins de 60 secondes
     */
    InscriptionEnAttenteModel renvoyerCode(String email);
}
