package com.proxymed.auth.mappers;

import com.proxymed.auth.service.model.CreationCompteModel;
import com.proxymed.auth.service.model.CreationCompteRequest;
import com.proxymed.auth.service.model.PageResponse;
import com.proxymed.auth.service.model.UtilisateurModel;
import com.proxymed.auth.service.model.UtilisateurResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Mapper API : convertit uniquement Request -> Model et Model -> Response.
 * Ne doit jamais connaitre l'entite JPA Utilisateur.
 */
@Component("apiAdminUtilisateurMapper")
@RequiredArgsConstructor
public class AdminUtilisateurMapper {

    private final AuthMapper authMapper;

    public CreationCompteModel toModel(CreationCompteRequest req) {
        return CreationCompteModel.builder()
                .nomComplet(req.nomComplet())
                .email(req.email())
                .role(req.role())
                .build();
    }

    public UtilisateurResponse toResponse(UtilisateurModel model) {
        return authMapper.toResponse(model);
    }

    public PageResponse<UtilisateurResponse> toResponse(Page<UtilisateurModel> page) {
        return new PageResponse<>(
                page.getContent().stream().map(authMapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
