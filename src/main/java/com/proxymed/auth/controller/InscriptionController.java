package com.proxymed.auth.controller;

import com.proxymed.auth.mappers.AuthMapper;
import com.proxymed.auth.mappers.InscriptionMapper;
import com.proxymed.auth.service.InscriptionService;
import com.proxymed.auth.service.model.InscriptionEnAttenteResponse;
import com.proxymed.auth.service.model.InscriptionRequest;
import com.proxymed.auth.service.model.RenvoyerCodeRequest;
import com.proxymed.auth.service.model.UtilisateurResponse;
import com.proxymed.auth.service.model.VerificationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inscription publique d'un UTILISATEUR (patient ou proche). Routes publiques declarees dans
 * WebSecurityConfig ; pas de @PreAuthorize puisque l'appelant n'a pas encore de compte.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Inscription", description = "Inscription d'un utilisateur avec code recu par email")
public class InscriptionController {

    private final InscriptionService inscriptionService;
    private final InscriptionMapper inscriptionMapper;
    private final AuthMapper authMapper;

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "S'inscrire (etape 1/2)",
            description = "Envoie un code a 6 chiffres par email, valable 10 minutes. Le compte n'est pas "
                    + "encore cree. 409 si un compte existe deja avec cet email, 429 si un code a ete "
                    + "envoye il y a moins de 60 secondes.")
    public InscriptionEnAttenteResponse inscription(@Valid @RequestBody InscriptionRequest request) {
        return inscriptionMapper.toResponse(inscriptionService.inscrire(inscriptionMapper.toModel(request)));
    }

    @PostMapping("/verification")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Valider le code (etape 2/2)",
            description = "Cree le compte avec le role UTILISATEUR. 400 si le code est faux ou expire ; "
                    + "apres 5 codes faux, l'inscription est annulee.")
    public UtilisateurResponse verification(@Valid @RequestBody VerificationRequest request) {
        return authMapper.toResponse(inscriptionService.verifier(inscriptionMapper.toModel(request)));
    }

    @PostMapping("/renvoyer-code")
    @Operation(summary = "Recevoir un nouveau code",
            description = "Remplace le code precedent. 429 si un code a ete envoye il y a moins de 60 secondes.")
    public InscriptionEnAttenteResponse renvoyerCode(@Valid @RequestBody RenvoyerCodeRequest request) {
        return inscriptionMapper.toResponse(inscriptionService.renvoyerCode(request.email()));
    }
}
