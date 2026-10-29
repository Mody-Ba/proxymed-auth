package com.proxymed.auth.service;

import com.proxymed.auth.enums.Role;
import com.proxymed.auth.repository.UtilisateurRepository;
import com.proxymed.auth.service.mappers.UtilisateurMapper;
import com.proxymed.auth.service.model.MedecinJuniorModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedecinServiceImpl implements MedecinService {

    /** Tri par nom ; l'id departage les homonymes pour un ordre stable. */
    private static final Sort TRI_PAR_NOM = Sort.by(Sort.Order.asc("nomComplet"), Sort.Order.asc("id"));

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;

    @Override
    public List<MedecinJuniorModel> listerJuniorsActifs() {
        return utilisateurRepository.findByRoleAndActifTrue(Role.MEDECIN_JUNIOR, TRI_PAR_NOM).stream()
                .map(utilisateurMapper::toMedecinJuniorModel)
                .toList();
    }
}
