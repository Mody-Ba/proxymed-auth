package com.proxymed.auth.service;

import com.proxymed.auth.entity.InscriptionEnAttente;
import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.enums.Role;
import com.proxymed.auth.exception.CodeVerificationException;
import com.proxymed.auth.exception.CompteExistantException;
import com.proxymed.auth.exception.ResourceNotFoundException;
import com.proxymed.auth.exception.TropDeDemandesException;
import com.proxymed.auth.repository.InscriptionEnAttenteRepository;
import com.proxymed.auth.repository.UtilisateurRepository;
import com.proxymed.auth.service.email.EmailService;
import com.proxymed.auth.service.mappers.InscriptionEnAttenteMapper;
import com.proxymed.auth.service.mappers.UtilisateurMapper;
import com.proxymed.auth.service.model.InscriptionEnAttenteModel;
import com.proxymed.auth.service.model.InscriptionModel;
import com.proxymed.auth.service.model.UtilisateurModel;
import com.proxymed.auth.service.model.VerificationModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * Inscription d'un UTILISATEUR (patient ou proche) en deux temps : inscription en attente + code
 * par email, puis creation du compte a la validation du code. Le mot de passe et le code sont
 * haches avec BCrypt ; ni l'un ni l'autre n'est jamais logge.
 *
 * L'email est envoye dans la transaction : si l'envoi echoue, l'inscription en attente n'est pas
 * enregistree et le client peut simplement recommencer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InscriptionServiceImpl implements InscriptionService {

    static final Duration VALIDITE_CODE = Duration.ofMinutes(10);
    static final Duration DELAI_ENTRE_ENVOIS = Duration.ofSeconds(60);
    static final int ESSAIS_MAX = 5;

    private static final SecureRandom ALEATOIRE = new SecureRandom();

    private final InscriptionEnAttenteRepository inscriptionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final InscriptionEnAttenteMapper inscriptionMapper;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public InscriptionEnAttenteModel inscrire(InscriptionModel inscription) {
        String email = AuthServiceImpl.normaliserEmail(inscription.email());
        if (utilisateurRepository.existsByEmail(email)) {
            throw new CompteExistantException();
        }

        Instant maintenant = Instant.now();
        // Une nouvelle inscription remplace l'ancienne en attente, mais respecte le delai entre deux
        // envois : sans cela, s'inscrire a nouveau permettrait de contourner la limite de renvoyer-code.
        InscriptionEnAttente enAttente = inscriptionRepository.findByEmail(email)
                .map(existante -> verifierDelaiEntreEnvois(existante, maintenant))
                .orElseGet(() -> InscriptionEnAttente.builder().email(email).build());

        enAttente.setNomComplet(inscription.nomComplet().trim());
        enAttente.setMotDePasse(passwordEncoder.encode(inscription.motDePasse()));
        enAttente.setDateCreation(maintenant);
        String code = nouveauCode(enAttente, maintenant);
        inscriptionRepository.save(enAttente);

        emailService.envoyerCodeInscription(email, enAttente.getNomComplet(), code, VALIDITE_CODE);
        log.info("Inscription en attente enregistree : {}", enAttente.getId());
        return inscriptionMapper.toModel(enAttente, DELAI_ENTRE_ENVOIS);
    }

    /**
     * noRollbackFor : un mauvais code doit quand meme enregistrer l'essai echoue (ou la suppression
     * apres le 5e, ou si le compte existe deja), alors que l'exception renvoyee au client
     * annulerait sinon la transaction.
     */
    @Override
    @Transactional(noRollbackFor = {CodeVerificationException.class, CompteExistantException.class})
    public UtilisateurModel verifier(VerificationModel verification) {
        String email = AuthServiceImpl.normaliserEmail(verification.email());
        InscriptionEnAttente enAttente = getEnAttenteByEmail(email);

        if (!Instant.now().isBefore(enAttente.getCodeExpireLe())) {
            throw new CodeVerificationException("Le code a expire. Demandez un nouveau code.");
        }
        if (!passwordEncoder.matches(verification.code(), enAttente.getCodeHash())) {
            enregistrerEssaiEchoue(enAttente);
        }
        if (utilisateurRepository.existsByEmail(email)) {
            inscriptionRepository.delete(enAttente);
            throw new CompteExistantException();
        }

        Utilisateur utilisateur = utilisateurRepository.save(Utilisateur.builder()
                .email(email)
                .motDePasse(enAttente.getMotDePasse())
                .nomComplet(enAttente.getNomComplet())
                .role(Role.UTILISATEUR)
                .actif(true)
                .dateCreation(Instant.now())
                .build());
        inscriptionRepository.delete(enAttente);

        log.info("Inscription validee : compte {} cree", utilisateur.getId());
        return utilisateurMapper.toModel(utilisateur);
    }

    @Override
    public InscriptionEnAttenteModel renvoyerCode(String emailSaisi) {
        String email = AuthServiceImpl.normaliserEmail(emailSaisi);
        Instant maintenant = Instant.now();
        InscriptionEnAttente enAttente = verifierDelaiEntreEnvois(getEnAttenteByEmail(email), maintenant);

        String code = nouveauCode(enAttente, maintenant);
        inscriptionRepository.save(enAttente);

        emailService.envoyerCodeInscription(email, enAttente.getNomComplet(), code, VALIDITE_CODE);
        log.info("Nouveau code envoye pour l'inscription en attente {}", enAttente.getId());
        return inscriptionMapper.toModel(enAttente, DELAI_ENTRE_ENVOIS);
    }

    /** Toujours leve une CodeVerificationException ; supprime l'inscription au dernier essai. */
    private void enregistrerEssaiEchoue(InscriptionEnAttente enAttente) {
        int essaisEchoues = enAttente.getEssaisEchoues() + 1;
        if (essaisEchoues >= ESSAIS_MAX) {
            inscriptionRepository.delete(enAttente);
            log.info("Inscription en attente {} supprimee apres {} essais echoues", enAttente.getId(), ESSAIS_MAX);
            throw new CodeVerificationException("Code incorrect. Nombre maximal d'essais atteint : "
                    + "l'inscription est annulee, veuillez vous inscrire a nouveau.");
        }
        enAttente.setEssaisEchoues(essaisEchoues);
        inscriptionRepository.save(enAttente);
        throw new CodeVerificationException("Code incorrect. Il reste " + (ESSAIS_MAX - essaisEchoues) + " essai(s).");
    }

    /** Genere un code, en stocke le hash, redemarre la validite et remet les essais a zero. */
    private String nouveauCode(InscriptionEnAttente enAttente, Instant maintenant) {
        String code = String.format("%06d", ALEATOIRE.nextInt(1_000_000));
        enAttente.setCodeHash(passwordEncoder.encode(code));
        enAttente.setCodeExpireLe(maintenant.plus(VALIDITE_CODE));
        enAttente.setEssaisEchoues(0);
        enAttente.setDernierEnvoi(maintenant);
        return code;
    }

    private InscriptionEnAttente verifierDelaiEntreEnvois(InscriptionEnAttente enAttente, Instant maintenant) {
        Instant renvoiPossibleLe = enAttente.getDernierEnvoi().plus(DELAI_ENTRE_ENVOIS);
        if (maintenant.isBefore(renvoiPossibleLe)) {
            long millisRestantes = Duration.between(maintenant, renvoiPossibleLe).toMillis();
            throw new TropDeDemandesException(Math.max(1, (millisRestantes + 999) / 1000));
        }
        return enAttente;
    }

    /**
     * Seul point d'acces a l'entite JPA dans ce service : prive, jamais expose aux
     * controllers ni aux autres services.
     */
    private InscriptionEnAttente getEnAttenteByEmail(String email) {
        return inscriptionRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune inscription en attente pour cet email. Veuillez vous inscrire."));
    }
}
