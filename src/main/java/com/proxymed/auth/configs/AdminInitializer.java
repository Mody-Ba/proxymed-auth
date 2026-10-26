package com.proxymed.auth.configs;

import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.enums.Role;
import com.proxymed.auth.repository.UtilisateurRepository;
import com.proxymed.auth.service.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Cree le premier administrateur au demarrage, a partir des variables d'environnement
 * PROXYMED_ADMIN_EMAIL et PROXYMED_ADMIN_PASSWORD (aucune valeur dans le code ni dans
 * application.properties). Ne fait rien s'il existe deja un compte ADMIN. Si une variable
 * manque, ecrit un avertissement et laisse l'application demarrer.
 * Le mot de passe est chiffre avec BCrypt et n'est jamais ecrit dans les logs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    /** Limite de BCrypt, et taille maximale acceptee par /api/auth/connexion. */
    private static final int TAILLE_MAX_MOT_DE_PASSE_OCTETS = 72;

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    // ":" sans valeur : une variable absente donne une chaine vide au lieu d'empecher le demarrage.
    @Value("${PROXYMED_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${PROXYMED_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        boolean variablesPresentes = adminEmail != null && !adminEmail.isBlank()
                && adminPassword != null && !adminPassword.isBlank();

        if (utilisateurRepository.existsByRole(Role.ADMIN)) {
            if (variablesPresentes) {
                log.warn("Un administrateur existe deja : PROXYMED_ADMIN_EMAIL et PROXYMED_ADMIN_PASSWORD "
                        + "sont ignorees et peuvent etre retirees de l'environnement");
            }
            return;
        }
        if (!variablesPresentes) {
            log.warn("Aucun administrateur et PROXYMED_ADMIN_EMAIL / PROXYMED_ADMIN_PASSWORD absentes : "
                    + "aucun administrateur cree");
            return;
        }
        if (adminPassword.getBytes(StandardCharsets.UTF_8).length > TAILLE_MAX_MOT_DE_PASSE_OCTETS) {
            log.warn("PROXYMED_ADMIN_PASSWORD depasse {} octets : aucun administrateur cree",
                    TAILLE_MAX_MOT_DE_PASSE_OCTETS);
            return;
        }

        String email = AuthServiceImpl.normaliserEmail(adminEmail);
        if (utilisateurRepository.existsByEmail(email)) {
            log.warn("PROXYMED_ADMIN_EMAIL correspond deja a un compte non administrateur : aucun administrateur cree");
            return;
        }

        Utilisateur admin = utilisateurRepository.save(Utilisateur.builder()
                .email(email)
                .motDePasse(passwordEncoder.encode(adminPassword))
                .nomComplet("Administrateur")
                .role(Role.ADMIN)
                .actif(true)
                .dateCreation(Instant.now())
                .build());

        log.info("Premier administrateur cree : compte {}", admin.getId());
    }
}
