package com.proxymed.auth.mappers;

import com.proxymed.auth.service.model.ConnexionModel;
import com.proxymed.auth.service.model.ConnexionRequest;
import com.proxymed.auth.service.model.ChangementMotDePasseModel;
import com.proxymed.auth.service.model.ChangementMotDePasseRequest;
import com.proxymed.auth.service.model.ConnexionResponse;
import com.proxymed.auth.service.model.IdentifiantsModel;
import com.proxymed.auth.service.model.UtilisateurModel;
import com.proxymed.auth.service.model.UtilisateurResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper API : convertit uniquement Request -> Model et Model -> Response.
 * Ne doit jamais connaitre l'entite JPA Utilisateur.
 */
@Component("apiAuthMapper")
public class AuthMapper {

    public static final String TYPE_BADGE = "Bearer";

    public IdentifiantsModel toModel(ConnexionRequest req) {
        return IdentifiantsModel.builder()
                .email(req.email())
                .motDePasse(req.motDePasse())
                .build();
    }

    public ChangementMotDePasseModel toModel(ChangementMotDePasseRequest req) {
        return ChangementMotDePasseModel.builder()
                .ancienMotDePasse(req.ancienMotDePasse())
                .nouveauMotDePasse(req.nouveauMotDePasse())
                .build();
    }

    public ConnexionResponse toResponse(ConnexionModel model) {
        return ConnexionResponse.builder()
                .accessToken(model.accessToken())
                .type(TYPE_BADGE)
                .expireDansSecondes(model.expireDansSecondes())
                .utilisateurId(model.utilisateur().id())
                .email(model.utilisateur().email())
                .role(model.utilisateur().role())
                .motDePasseTemporaire(model.utilisateur().motDePasseTemporaire())
                .build();
    }

    public UtilisateurResponse toResponse(UtilisateurModel model) {
        return UtilisateurResponse.builder()
                .id(model.id())
                .email(model.email())
                .nomComplet(model.nomComplet())
                .role(model.role())
                .actif(model.actif())
                .motDePasseTemporaire(model.motDePasseTemporaire())
                .dateCreation(model.dateCreation())
                .build();
    }
}
