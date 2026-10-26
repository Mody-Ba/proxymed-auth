package com.proxymed.auth.controller;

import com.proxymed.auth.configs.security.UtilisateurConnecte;
import com.proxymed.auth.mappers.AuthMapper;
import com.proxymed.auth.service.AuthService;
import com.proxymed.auth.service.model.ConnexionRequest;
import com.proxymed.auth.service.model.ConnexionResponse;
import com.proxymed.auth.service.model.UtilisateurResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Connexion et badge (JWT)")
public class AuthController {

    private final AuthService authService;
    private final AuthMapper authMapper;

    @PostMapping("/connexion")
    @SecurityRequirements
    @Operation(summary = "Se connecter avec email et mot de passe",
            description = "Renvoie un badge (accessToken) a envoyer ensuite dans l'en-tete "
                    + "\"Authorization: Bearer ...\". 401 si les identifiants sont faux, 403 si le compte est inactif.")
    public ConnexionResponse connexion(@Valid @RequestBody ConnexionRequest request) {
        return authMapper.toResponse(authService.connexion(authMapper.toModel(request)));
    }

    @GetMapping("/moi")
    @Operation(summary = "Compte connecte", description = "Renvoie le compte correspondant au badge envoye.")
    public UtilisateurResponse moi(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return authMapper.toResponse(authService.findById(utilisateur.id()));
    }
}
