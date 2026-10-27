package com.proxymed.auth.service.model;

import com.proxymed.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Creation d'un compte medecin par un ADMIN. Pas de mot de passe : le serveur en genere un
 * temporaire et l'envoie par email au medecin.
 */
public record CreationCompteRequest(
        @NotBlank(message = "le nom complet est obligatoire")
        @Size(max = 255, message = "le nom complet ne doit pas depasser 255 caracteres")
        String nomComplet,

        @NotBlank(message = "l'email est obligatoire")
        @Email(message = "l'email n'est pas valide")
        @Size(max = 255, message = "l'email ne doit pas depasser 255 caracteres")
        String email,

        @NotNull(message = "le role est obligatoire (MEDECIN_JUNIOR ou MEDECIN_SENIOR)")
        Role role
) {
}
