package com.proxymed.auth.service;

import com.proxymed.auth.service.model.ChangementMotDePasseModel;
import com.proxymed.auth.service.model.ConnexionModel;
import com.proxymed.auth.service.model.IdentifiantsModel;
import com.proxymed.auth.service.model.UtilisateurModel;

import java.util.UUID;

public interface AuthService {

    /**
     * Verifie les identifiants et delivre un badge.
     *
     * @throws com.proxymed.auth.exception.IdentifiantsInvalidesException email inconnu ou mot de passe faux
     * @throws com.proxymed.auth.exception.CompteInactifException identifiants corrects mais compte desactive
     */
    ConnexionModel connexion(IdentifiantsModel identifiants);

    UtilisateurModel findById(UUID id);

    /**
     * Remplace le mot de passe du compte connecte et leve le marqueur "mot de passe temporaire".
     *
     * @throws com.proxymed.auth.exception.RegleGestionException ancien mot de passe faux, ou nouveau identique
     * @throws com.proxymed.auth.exception.CompteInactifException compte desactive entre-temps
     */
    void changerMotDePasse(UUID utilisateurId, ChangementMotDePasseModel changement);
}
