package com.proxymed.auth.controller;

import com.proxymed.auth.mappers.MedecinMapper;
import com.proxymed.auth.service.MedecinService;
import com.proxymed.auth.service.model.MedecinJuniorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Liste des medecins juniors pour le MEDECIN_SENIOR (verifie ici par @PreAuthorize,
 * pas dans WebSecurityConfig). N'expose que l'identifiant et le nom.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Medecins", description = "Choix d'un medecin junior par un MEDECIN_SENIOR")
public class MedecinController {

    private final MedecinService medecinService;
    private final MedecinMapper medecinMapper;

    @GetMapping("/medecins-juniors")
    @PreAuthorize("hasRole('MEDECIN_SENIOR')")
    @Operation(summary = "Lister les medecins juniors actifs",
            description = "Reserve au role MEDECIN_SENIOR. Tries par nom ; seuls id et nomComplet sont renvoyes. "
                    + "Les comptes desactives n'apparaissent pas.")
    public List<MedecinJuniorResponse> listerJuniors() {
        return medecinMapper.toResponse(medecinService.listerJuniorsActifs());
    }
}
