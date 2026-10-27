package com.proxymed.auth.service;

import com.proxymed.auth.configs.JwtUtils;
import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.exception.CompteInactifException;
import com.proxymed.auth.exception.IdentifiantsInvalidesException;
import com.proxymed.auth.exception.RegleGestionException;
import com.proxymed.auth.exception.ResourceNotFoundException;
import com.proxymed.auth.repository.UtilisateurRepository;
import com.proxymed.auth.service.mappers.UtilisateurMapper;
import com.proxymed.auth.service.model.ChangementMotDePasseModel;
import com.proxymed.auth.service.model.ConnexionModel;
import com.proxymed.auth.service.model.IdentifiantsModel;
import com.proxymed.auth.service.model.UtilisateurModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * Hash BCrypt compare quand l'email est inconnu : la reponse prend alors autant de temps
     * qu'avec un mauvais mot de passe, ce qui empeche de deviner les emails existants au chronometre.
     */
    private String hashLeurre;

    @Override
    public ConnexionModel connexion(IdentifiantsModel identifiants) {
        String email = normaliserEmail(identifiants.email());
        Optional<Utilisateur> compte = utilisateurRepository.findByEmail(email);

        if (compte.isEmpty()) {
            passwordEncoder.matches(identifiants.motDePasse(), hashLeurre());
            log.info("Connexion refusee : identifiants invalides");
            throw new IdentifiantsInvalidesException();
        }
        Utilisateur utilisateur = compte.get();
        if (!passwordEncoder.matches(identifiants.motDePasse(), utilisateur.getMotDePasse())) {
            log.info("Connexion refusee : identifiants invalides (compte {})", utilisateur.getId());
            throw new IdentifiantsInvalidesException();
        }
        // Verifie apres le mot de passe : sans le bon mot de passe, on ne revele pas qu'un compte
        // inactif existe pour cet email.
        if (!utilisateur.isActif()) {
            log.info("Connexion refusee : compte inactif {}", utilisateur.getId());
            throw new CompteInactifException();
        }

        String token = jwtUtils.genererToken(utilisateur.getId(), utilisateur.getEmail(),
                List.of(utilisateur.getRole().name()));
        log.info("Connexion reussie : compte {}", utilisateur.getId());
        return ConnexionModel.builder()
                .accessToken(token)
                .expireDansSecondes(jwtUtils.getExpirationSecondes())
                .utilisateur(utilisateurMapper.toModel(utilisateur))
                .build();
    }

    @Override
    public UtilisateurModel findById(UUID id) {
        return utilisateurMapper.toModel(getEntityById(id));
    }

    @Override
    @Transactional
    public void changerMotDePasse(UUID utilisateurId, ChangementMotDePasseModel changement) {
        Utilisateur utilisateur = getEntityById(utilisateurId);
        // Le badge reste valable jusqu'a son expiration : on reverifie l'etat du compte en base.
        if (!utilisateur.isActif()) {
            throw new CompteInactifException();
        }
        if (!passwordEncoder.matches(changement.ancienMotDePasse(), utilisateur.getMotDePasse())) {
            log.info("Changement de mot de passe refuse : ancien mot de passe incorrect (compte {})", utilisateurId);
            throw new RegleGestionException("L'ancien mot de passe est incorrect");
        }
        if (changement.nouveauMotDePasse().equals(changement.ancienMotDePasse())) {
            throw new RegleGestionException("Le nouveau mot de passe doit etre different de l'ancien");
        }
        utilisateur.setMotDePasse(passwordEncoder.encode(changement.nouveauMotDePasse()));
        utilisateur.setMotDePasseTemporaire(false);
        log.info("Mot de passe change : compte {}", utilisateurId);
    }

    /** Point unique de normalisation : les emails sont stockes et recherches en minuscules. */
    public static String normaliserEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Seul point d'acces a l'entite JPA dans ce service : prive, jamais expose aux
     * controllers ni aux autres services.
     */
    private Utilisateur getEntityById(UUID id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + id));
    }

    private String hashLeurre() {
        if (hashLeurre == null) {
            hashLeurre = passwordEncoder.encode(UUID.randomUUID().toString());
        }
        return hashLeurre;
    }
}
