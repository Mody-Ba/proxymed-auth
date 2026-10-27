package com.proxymed.auth.service;

import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.enums.Role;
import com.proxymed.auth.exception.CompteExistantException;
import com.proxymed.auth.exception.RegleGestionException;
import com.proxymed.auth.exception.ResourceNotFoundException;
import com.proxymed.auth.repository.UtilisateurRepository;
import com.proxymed.auth.service.email.EmailService;
import com.proxymed.auth.service.mappers.UtilisateurMapper;
import com.proxymed.auth.service.model.CreationCompteModel;
import com.proxymed.auth.service.model.UtilisateurModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Le mot de passe temporaire n'existe en clair que le temps de le hacher et de l'envoyer par email :
 * il n'est ni renvoye au client ni logge.
 *
 * L'email est envoye dans la transaction, apres un flush : si l'envoi echoue, la transaction est
 * annulee et rien n'est enregistre (creation) ou l'ancien mot de passe reste valable (reinitialisation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUtilisateurServiceImpl implements AdminUtilisateurService {

    /** Seuls les comptes medecins sont crees par un ADMIN ; les UTILISATEUR s'inscrivent eux-memes. */
    static final Set<Role> ROLES_CREABLES = Set.of(Role.MEDECIN_JUNIOR, Role.MEDECIN_SENIOR);

    private static final Sort TRI_LISTE = Sort.by(Sort.Order.desc("dateCreation"), Sort.Order.asc("id"));

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;
    private final GenerateurMotDePasse generateurMotDePasse;
    private final EmailService emailService;

    @Override
    public UtilisateurModel creerCompteMedecin(CreationCompteModel creation) {
        if (!ROLES_CREABLES.contains(creation.role())) {
            throw new RegleGestionException("Un administrateur ne peut creer que des comptes MEDECIN_JUNIOR "
                    + "ou MEDECIN_SENIOR (role demande : " + creation.role() + ")");
        }
        String email = AuthServiceImpl.normaliserEmail(creation.email());
        if (utilisateurRepository.existsByEmail(email)) {
            throw new CompteExistantException();
        }

        String motDePasseTemporaire = generateurMotDePasse.generer();
        Utilisateur utilisateur = utilisateurRepository.saveAndFlush(Utilisateur.builder()
                .email(email)
                .motDePasse(passwordEncoder.encode(motDePasseTemporaire))
                .nomComplet(creation.nomComplet().trim())
                .role(creation.role())
                .actif(true)
                .motDePasseTemporaire(true)
                .dateCreation(Instant.now())
                .build());

        emailService.envoyerMotDePasseTemporaire(email, utilisateur.getNomComplet(), motDePasseTemporaire);
        log.info("Compte {} cree par un administrateur : {}", utilisateur.getRole(), utilisateur.getId());
        return utilisateurMapper.toModel(utilisateur);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UtilisateurModel> lister(Role role, int page, int taille) {
        Pageable pageable = PageRequest.of(page, taille, TRI_LISTE);
        Page<Utilisateur> comptes = role != null
                ? utilisateurRepository.findByRole(role, pageable)
                : utilisateurRepository.findAll(pageable);
        return comptes.map(utilisateurMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurModel findById(UUID id) {
        return utilisateurMapper.toModel(getEntityById(id));
    }

    @Override
    public UtilisateurModel changerActivation(UUID id, boolean actif, UUID adminConnecteId) {
        if (!actif && id.equals(adminConnecteId)) {
            throw new RegleGestionException("Un administrateur ne peut pas desactiver son propre compte");
        }
        Utilisateur utilisateur = getEntityById(id);
        utilisateur.setActif(actif);
        log.info("Compte {} {} par un administrateur", id, actif ? "active" : "desactive");
        return utilisateurMapper.toModel(utilisateur);
    }

    @Override
    public UtilisateurModel reinitialiserMotDePasse(UUID id) {
        Utilisateur utilisateur = getEntityById(id);
        String motDePasseTemporaire = generateurMotDePasse.generer();
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasseTemporaire));
        utilisateur.setMotDePasseTemporaire(true);
        utilisateurRepository.saveAndFlush(utilisateur);

        emailService.envoyerMotDePasseTemporaire(utilisateur.getEmail(), utilisateur.getNomComplet(), motDePasseTemporaire);
        log.info("Mot de passe du compte {} reinitialise par un administrateur", id);
        return utilisateurMapper.toModel(utilisateur);
    }

    /**
     * Seul point d'acces a l'entite JPA dans ce service : prive, jamais expose aux
     * controllers ni aux autres services.
     */
    private Utilisateur getEntityById(UUID id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + id));
    }
}
