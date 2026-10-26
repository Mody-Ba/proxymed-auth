package com.proxymed.auth.service.mappers;

import com.proxymed.auth.entity.InscriptionEnAttente;
import com.proxymed.auth.service.model.InscriptionEnAttenteModel;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Mapper DB : convertit uniquement l'entite JPA InscriptionEnAttente en InscriptionEnAttenteModel.
 * Ne doit jamais connaitre InscriptionEnAttenteResponse. Ni le code ni les hashes ne sont recopies.
 */
@Component("dbInscriptionEnAttenteMapper")
public class InscriptionEnAttenteMapper {

    public InscriptionEnAttenteModel toModel(InscriptionEnAttente entity, Duration delaiEntreEnvois) {
        return InscriptionEnAttenteModel.builder()
                .email(entity.getEmail())
                .codeExpireLe(entity.getCodeExpireLe())
                .renvoiPossibleLe(entity.getDernierEnvoi().plus(delaiEntreEnvois))
                .build();
    }
}
