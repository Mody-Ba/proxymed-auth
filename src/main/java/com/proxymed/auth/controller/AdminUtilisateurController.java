package com.proxymed.auth.controller;

import com.proxymed.auth.configs.security.UtilisateurConnecte;
import com.proxymed.auth.enums.Role;
import com.proxymed.auth.mappers.AdminUtilisateurMapper;
import com.proxymed.auth.service.AdminUtilisateurService;
import com.proxymed.auth.service.model.ActivationRequest;
import com.proxymed.auth.service.model.CreationCompteRequest;
import com.proxymed.auth.service.model.PageResponse;
import com.proxymed.auth.service.model.UtilisateurResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Gestion des comptes, reservee a l'ADMIN (verifie ici par @PreAuthorize sur chaque methode,
 * pas dans WebSecurityConfig). Aucune donnee medicale.
 */
@RestController
@RequestMapping("/api/admin/utilisateurs")
@RequiredArgsConstructor
@Tag(name = "Administration des comptes", description = "Reserve au role ADMIN")
public class AdminUtilisateurController {

    private final AdminUtilisateurService adminUtilisateurService;
    private final AdminUtilisateurMapper adminUtilisateurMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creer un compte medecin",
            description = "Role MEDECIN_JUNIOR ou MEDECIN_SENIOR uniquement. Un mot de passe temporaire est "
                    + "genere et envoye par email au medecin ; il n'est jamais renvoye ici.")
    public UtilisateurResponse creer(@Valid @RequestBody CreationCompteRequest request) {
        return adminUtilisateurMapper.toResponse(
                adminUtilisateurService.creerCompteMedecin(adminUtilisateurMapper.toModel(request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les comptes", description = "Pagine, du plus recent au plus ancien ; filtre optionnel par role.")
    public PageResponse<UtilisateurResponse> lister(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page doit etre positif ou nul") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "taille doit etre au moins 1")
            @Max(value = 100, message = "taille ne doit pas depasser 100") int taille) {
        return adminUtilisateurMapper.toResponse(adminUtilisateurService.lister(role, page, taille));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Detail d'un compte")
    public UtilisateurResponse findById(@PathVariable UUID id) {
        return adminUtilisateurMapper.toResponse(adminUtilisateurService.findById(id));
    }

    @PatchMapping("/{id}/activation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer ou desactiver un compte",
            description = "Un compte desactive ne peut plus se connecter. Un ADMIN ne peut pas desactiver son propre compte.")
    public UtilisateurResponse changerActivation(
            @PathVariable UUID id,
            @Valid @RequestBody ActivationRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte admin) {
        return adminUtilisateurMapper.toResponse(
                adminUtilisateurService.changerActivation(id, request.actif(), admin.id()));
    }

    @PostMapping("/{id}/reinitialiser-mot-de-passe")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reinitialiser le mot de passe",
            description = "Genere un nouveau mot de passe temporaire et l'envoie par email au titulaire du compte.")
    public UtilisateurResponse reinitialiserMotDePasse(@PathVariable UUID id) {
        return adminUtilisateurMapper.toResponse(adminUtilisateurService.reinitialiserMotDePasse(id));
    }
}
