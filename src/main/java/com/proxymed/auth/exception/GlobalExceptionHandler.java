package com.proxymed.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.List;

/**
 * Les erreurs 401/403 levees par Spring Security AVANT d'atteindre un controller (badge absent,
 * invalide, acces refuse) sont traitees par AuthEntryPointJwt et AccesRefuseHandler, avec le
 * meme format ErrorResponse.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ResponseEntity<ErrorResponse> handleIdentifiantsInvalides(IdentifiantsInvalidesException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Authentification echouee", ex.getMessage()));
    }

    @ExceptionHandler(CompteInactifException.class)
    public ResponseEntity<ErrorResponse> handleCompteInactif(CompteInactifException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Compte inactif", ex.getMessage()));
    }

    /**
     * Refus d'un @PreAuthorize("hasRole('...')") : l'exception est levee dans le controller, donc
     * elle arrive ici et non dans AccesRefuseHandler. Sans ce handler, le filet de securite
     * Exception.class la transformerait en 500. Meme reponse que AccesRefuseHandler.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesRefuse(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Acces refuse",
                        "Votre role ne permet pas d'acceder a cette ressource"));
    }

    @ExceptionHandler(CompteExistantException.class)
    public ResponseEntity<ErrorResponse> handleCompteExistant(CompteExistantException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Compte existant", ex.getMessage()));
    }

    @ExceptionHandler(CodeVerificationException.class)
    public ResponseEntity<ErrorResponse> handleCodeVerification(CodeVerificationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Code de verification refuse", ex.getMessage()));
    }

    @ExceptionHandler(TropDeDemandesException.class)
    public ResponseEntity<ErrorResponse> handleTropDeDemandes(TropDeDemandesException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSecondesAAttendre()))
                .body(new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), "Trop de demandes", ex.getMessage()));
    }

    @ExceptionHandler(EnvoiEmailException.class)
    public ResponseEntity<ErrorResponse> handleEnvoiEmail(EnvoiEmailException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), "Envoi d'email impossible", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Ressource introuvable", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Seuls le champ et le message de la contrainte sont renvoyes, jamais la valeur rejetee
        // (qui pourrait etre un mot de passe).
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation echouee", "Un ou plusieurs champs sont invalides", details));
    }

    /**
     * Corps JSON illisible. Cas le plus courant : un champ inconnu du DTO (rejete grace a
     * spring.jackson.deserialization.fail-on-unknown-properties=true).
     * On nomme le champ fautif pour que le client sache quoi corriger.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = ex.getMostSpecificCause() instanceof UnrecognizedPropertyException upe
                ? "Champ inconnu dans la requete : " + upe.getPropertyName()
                : "Le corps de la requete est illisible ou mal forme";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Requete invalide", message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.METHOD_NOT_ALLOWED.value(), "Methode non autorisee",
                        "La methode " + ex.getMethod() + " n'est pas acceptee sur cette adresse"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Ressource introuvable",
                        "Aucune ressource a l'adresse /" + ex.getResourcePath()));
    }

    /**
     * Filet de securite : le detail technique reste dans les logs, le client recoit un message neutre.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInattendue(Exception ex) {
        log.error("Erreur inattendue", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erreur interne",
                        "Une erreur inattendue est survenue"));
    }
}
