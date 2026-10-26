package com.proxymed.auth.mappers;

import com.proxymed.auth.service.model.InscriptionEnAttenteModel;
import com.proxymed.auth.service.model.InscriptionEnAttenteResponse;
import com.proxymed.auth.service.model.InscriptionModel;
import com.proxymed.auth.service.model.InscriptionRequest;
import com.proxymed.auth.service.model.VerificationModel;
import com.proxymed.auth.service.model.VerificationRequest;
import org.springframework.stereotype.Component;

/**
 * Mapper API : convertit uniquement Request -> Model et Model -> Response.
 * Ne doit jamais connaitre l'entite JPA InscriptionEnAttente.
 */
@Component("apiInscriptionMapper")
public class InscriptionMapper {

    public InscriptionModel toModel(InscriptionRequest req) {
        return InscriptionModel.builder()
                .nomComplet(req.nomComplet())
                .email(req.email())
                .motDePasse(req.motDePasse())
                .build();
    }

    public VerificationModel toModel(VerificationRequest req) {
        return VerificationModel.builder()
                .email(req.email())
                .code(req.code())
                .build();
    }

    public InscriptionEnAttenteResponse toResponse(InscriptionEnAttenteModel model) {
        return InscriptionEnAttenteResponse.builder()
                .message("Un code de verification a ete envoye a " + model.email())
                .email(model.email())
                .codeExpireLe(model.codeExpireLe())
                .renvoiPossibleLe(model.renvoiPossibleLe())
                .build();
    }
}
