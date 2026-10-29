package com.proxymed.auth.mappers;

import com.proxymed.auth.service.model.MedecinJuniorModel;
import com.proxymed.auth.service.model.MedecinJuniorResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper API : convertit uniquement Model -> Response.
 * Ne doit jamais connaitre l'entite JPA Utilisateur.
 */
@Component("apiMedecinMapper")
public class MedecinMapper {

    public MedecinJuniorResponse toResponse(MedecinJuniorModel model) {
        return MedecinJuniorResponse.builder()
                .id(model.id())
                .nomComplet(model.nomComplet())
                .build();
    }

    public List<MedecinJuniorResponse> toResponse(List<MedecinJuniorModel> models) {
        return models.stream().map(this::toResponse).toList();
    }
}
