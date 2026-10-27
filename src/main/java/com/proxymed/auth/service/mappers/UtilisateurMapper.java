package com.proxymed.auth.service.mappers;

import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.service.model.UtilisateurModel;
import org.springframework.stereotype.Component;

/**
 * Mapper DB : convertit uniquement l'entite JPA Utilisateur en UtilisateurModel.
 * Ne doit jamais connaitre UtilisateurResponse. Le hash du mot de passe n'est pas recopie.
 */
@Component("dbUtilisateurMapper")
public class UtilisateurMapper {

    public UtilisateurModel toModel(Utilisateur entity) {
        return UtilisateurModel.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .nomComplet(entity.getNomComplet())
                .role(entity.getRole())
                .actif(entity.isActif())
                .motDePasseTemporaire(entity.isMotDePasseTemporaire())
                .dateCreation(entity.getDateCreation())
                .build();
    }
}
