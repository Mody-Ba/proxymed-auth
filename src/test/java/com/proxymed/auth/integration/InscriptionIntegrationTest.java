package com.proxymed.auth.integration;

import com.proxymed.auth.entity.InscriptionEnAttente;
import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.enums.Role;
import com.proxymed.auth.exception.EnvoiEmailException;
import com.proxymed.auth.repository.InscriptionEnAttenteRepository;
import com.proxymed.auth.repository.UtilisateurRepository;
import com.proxymed.auth.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EmailService est remplace par un mock : aucun email n'est envoye. Le code envoye est
 * recupere sur le mock, comme l'utilisateur le lirait dans sa boite mail.
 */
@SpringBootTest(properties = "logging.level.com.proxymed.auth=DEBUG")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class InscriptionIntegrationTest {

    private static final String EMAIL = "fatou.sow@proxymed.test";
    private static final String NOM = "Fatou Sow";
    private static final String MDP = "Patient#Secret42";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InscriptionEnAttenteRepository inscriptionRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        inscriptionRepository.deleteAll();
        utilisateurRepository.deleteAll();
    }

    // --- POST /api/auth/inscription ---

    @Test
    void inscription_enregistreUneInscriptionEnAttenteEtEnvoieLeCode(CapturedOutput output) throws Exception {
        String corps = inscrire(NOM, EMAIL, MDP)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.email", is(EMAIL)))
                .andExpect(jsonPath("$.message", containsString(EMAIL)))
                .andExpect(jsonPath("$.codeExpireLe", notNullValue()))
                .andExpect(jsonPath("$.renvoiPossibleLe", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String code = dernierCodeEnvoye();
        assertThat(code).matches("\\d{6}");
        verify(emailService).envoyerCodeInscription(eq(EMAIL), eq(NOM), eq(code), eq(Duration.ofMinutes(10)));

        // Aucun compte avant validation du code.
        assertThat(utilisateurRepository.count()).isZero();
        InscriptionEnAttente enAttente = inscriptionRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(enAttente.getMotDePasse()).startsWith("$2a$").isNotEqualTo(MDP);
        assertThat(passwordEncoder.matches(MDP, enAttente.getMotDePasse())).isTrue();
        assertThat(enAttente.getCodeHash()).startsWith("$2a$").isNotEqualTo(code);
        assertThat(passwordEncoder.matches(code, enAttente.getCodeHash())).isTrue();
        assertThat(Duration.between(Instant.now(), enAttente.getCodeExpireLe()))
                .isBetween(Duration.ofMinutes(9), Duration.ofMinutes(10));

        assertThat(corps).doesNotContain(MDP).doesNotContain(code);
        assertThat(output.getAll()).doesNotContain(MDP).doesNotContain(code);
    }

    @Test
    void inscription_emailEnMajuscules_estNormalise() throws Exception {
        inscrire(NOM, "Fatou.SOW@Proxymed.Test", MDP).andExpect(status().isAccepted());

        assertThat(inscriptionRepository.findByEmail(EMAIL)).isPresent();
    }

    @Test
    void inscription_compteExistant_409() throws Exception {
        utilisateurRepository.save(Utilisateur.builder()
                .email(EMAIL).motDePasse(passwordEncoder.encode(MDP)).nomComplet(NOM)
                .role(Role.UTILISATEUR).actif(true).dateCreation(Instant.now()).build());

        inscrire(NOM, EMAIL, MDP)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.erreur", is("Compte existant")))
                .andExpect(jsonPath("$.message", is("Un compte existe deja avec cet email")));

        assertThat(inscriptionRepository.count()).isZero();
        verify(emailService, never()).envoyerCodeInscription(any(), any(), any(), any());
    }

    @Test
    void inscription_motDePasseTropCourtOuTropLong_400() throws Exception {
        inscrire(NOM, EMAIL, "Court#1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Validation echouee")))
                .andExpect(jsonPath("$.details[0]", containsString("entre 8 et 72 caracteres")));
        inscrire(NOM, EMAIL, "x".repeat(73)).andExpect(status().isBadRequest());

        assertThat(inscriptionRepository.count()).isZero();
    }

    @Test
    void inscription_champsInvalides_400AvecDetails() throws Exception {
        inscrire("", "pas-un-email", MDP)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()", is(2)));
    }

    @Test
    void inscription_champInconnu_400() throws Exception {
        mockMvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomComplet\":\"" + NOM + "\",\"email\":\"" + EMAIL + "\",\"motDePasse\":\""
                                + MDP + "\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Champ inconnu dans la requete : role")));
    }

    @Test
    void inscription_memeEmail_remplaceLAncienne() throws Exception {
        inscrire(NOM, EMAIL, MDP).andExpect(status().isAccepted());
        String ancienCode = dernierCodeEnvoye();
        dernierEnvoiIlYA(Duration.ofSeconds(61));
        clearInvocations(emailService);

        inscrire("Fatou Sow Diallo", EMAIL, "Nouveau#Secret42").andExpect(status().isAccepted());
        String nouveauCode = dernierCodeEnvoye();

        assertThat(inscriptionRepository.count()).isEqualTo(1);
        InscriptionEnAttente enAttente = inscriptionRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(enAttente.getNomComplet()).isEqualTo("Fatou Sow Diallo");
        assertThat(passwordEncoder.matches("Nouveau#Secret42", enAttente.getMotDePasse())).isTrue();
        if (!ancienCode.equals(nouveauCode)) {
            assertThat(passwordEncoder.matches(ancienCode, enAttente.getCodeHash())).isFalse();
        }
        verifier(EMAIL, nouveauCode).andExpect(status().isCreated());
    }

    @Test
    void inscription_memeEmailMoinsDe60Secondes_429() throws Exception {
        inscrire(NOM, EMAIL, MDP).andExpect(status().isAccepted());

        inscrire(NOM, EMAIL, MDP)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status", is(429)));
        verify(emailService, times(1)).envoyerCodeInscription(any(), any(), any(), any());
    }

    @Test
    void inscription_echecDEnvoiDeLEmail_503EtRienNEstEnregistre() throws Exception {
        doThrow(new EnvoiEmailException(new RuntimeException("SMTP indisponible")))
                .when(emailService).envoyerCodeInscription(anyString(), anyString(), anyString(), any());

        inscrire(NOM, EMAIL, MDP)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", is(503)))
                .andExpect(jsonPath("$.message", containsString("n'a pas pu etre envoye")));

        assertThat(inscriptionRepository.count()).isZero();
    }

    // --- POST /api/auth/verification ---

    @Test
    void verification_codeCorrect_201CreeLeCompteUtilisateur() throws Exception {
        inscrire(NOM, EMAIL, MDP);
        String code = dernierCodeEnvoye();

        String corps = verifier(EMAIL, code)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is(EMAIL)))
                .andExpect(jsonPath("$.nomComplet", is(NOM)))
                .andExpect(jsonPath("$.role", is("UTILISATEUR")))
                .andExpect(jsonPath("$.actif", is(true)))
                .andReturn().getResponse().getContentAsString();

        assertThat(corps).doesNotContain(MDP).doesNotContainIgnoringCase("motDePasse");
        assertThat(inscriptionRepository.count()).isZero();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(utilisateur.getRole()).isEqualTo(Role.UTILISATEUR);

        // Le nouveau compte peut se connecter avec le mot de passe choisi a l'inscription.
        mockMvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", EMAIL, "motDePasse", MDP))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("UTILISATEUR")));
    }

    @Test
    void verification_mauvaisCode_400EtCompteLesEssais() throws Exception {
        inscrire(NOM, EMAIL, MDP);
        String faux = codeDifferentDe(dernierCodeEnvoye());

        verifier(EMAIL, faux)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Code de verification refuse")))
                .andExpect(jsonPath("$.message", is("Code incorrect. Il reste 4 essai(s).")));

        assertThat(inscriptionRepository.findByEmail(EMAIL).orElseThrow().getEssaisEchoues()).isEqualTo(1);
        assertThat(utilisateurRepository.count()).isZero();
    }

    @Test
    void verification_cinqMauvaisCodes_supprimeLInscriptionEnAttente() throws Exception {
        inscrire(NOM, EMAIL, MDP);
        String bon = dernierCodeEnvoye();
        String faux = codeDifferentDe(bon);

        for (int essai = 1; essai <= 4; essai++) {
            verifier(EMAIL, faux).andExpect(status().isBadRequest());
        }
        verifier(EMAIL, faux)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Nombre maximal d'essais atteint")));

        assertThat(inscriptionRepository.count()).isZero();
        // Meme le bon code ne sert plus : il faut se reinscrire.
        verifier(EMAIL, bon).andExpect(status().isNotFound());
        assertThat(utilisateurRepository.count()).isZero();
    }

    @Test
    void verification_codeExpire_400SansCreerDeCompte() throws Exception {
        inscrire(NOM, EMAIL, MDP);
        String code = dernierCodeEnvoye();
        InscriptionEnAttente enAttente = inscriptionRepository.findByEmail(EMAIL).orElseThrow();
        enAttente.setCodeExpireLe(Instant.now().minusSeconds(1));
        inscriptionRepository.save(enAttente);

        verifier(EMAIL, code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Le code a expire. Demandez un nouveau code.")));

        assertThat(utilisateurRepository.count()).isZero();
        assertThat(inscriptionRepository.count()).isEqualTo(1);
    }

    @Test
    void verification_sansInscriptionEnAttente_404() throws Exception {
        verifier(EMAIL, "123456")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Aucune inscription en attente")));
    }

    @Test
    void verification_codeMalForme_400() throws Exception {
        verifier(EMAIL, "12ab")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("6 chiffres")));
    }

    // --- POST /api/auth/renvoyer-code ---

    @Test
    void renvoyerCode_moinsDe60Secondes_429() throws Exception {
        inscrire(NOM, EMAIL, MDP);

        renvoyerCode(EMAIL)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.erreur", is("Trop de demandes")))
                .andExpect(jsonPath("$.message", containsString("Reessayez dans")));
        verify(emailService, times(1)).envoyerCodeInscription(any(), any(), any(), any());
    }

    @Test
    void renvoyerCode_apres60Secondes_envoieUnNouveauCodeQuiRemplaceLAncien() throws Exception {
        inscrire(NOM, EMAIL, MDP);
        String ancienCode = dernierCodeEnvoye();
        verifier(EMAIL, codeDifferentDe(ancienCode)).andExpect(status().isBadRequest());
        dernierEnvoiIlYA(Duration.ofSeconds(61));
        clearInvocations(emailService);

        renvoyerCode(EMAIL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(EMAIL)));
        String nouveauCode = dernierCodeEnvoye();

        InscriptionEnAttente enAttente = inscriptionRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(enAttente.getEssaisEchoues()).isZero();
        if (!ancienCode.equals(nouveauCode)) {
            verifier(EMAIL, ancienCode).andExpect(status().isBadRequest());
        }
        verifier(EMAIL, nouveauCode).andExpect(status().isCreated());
    }

    @Test
    void renvoyerCode_sansInscriptionEnAttente_404() throws Exception {
        renvoyerCode(EMAIL).andExpect(status().isNotFound());
        verify(emailService, never()).envoyerCodeInscription(any(), any(), any(), any());
    }

    // --- Outils ---

    private ResultActions inscrire(String nomComplet, String email, String motDePasse) throws Exception {
        return mockMvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nomComplet", nomComplet, "email", email, "motDePasse", motDePasse))));
    }

    private ResultActions verifier(String email, String code) throws Exception {
        return mockMvc.perform(post("/api/auth/verification").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "code", code))));
    }

    private ResultActions renvoyerCode(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/renvoyer-code").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email))));
    }

    private String json(Map<String, String> champs) {
        return objectMapper.writeValueAsString(new LinkedHashMap<>(champs));
    }

    /** Le code tel que l'utilisateur le recoit par email. */
    private String dernierCodeEnvoye() {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService, org.mockito.Mockito.atLeastOnce())
                .envoyerCodeInscription(anyString(), anyString(), code.capture(), any());
        return code.getValue();
    }

    /** Simule le passage du temps depuis le dernier envoi de code. */
    private void dernierEnvoiIlYA(Duration duree) {
        InscriptionEnAttente enAttente = inscriptionRepository.findByEmail(EMAIL).orElseThrow();
        enAttente.setDernierEnvoi(Instant.now().minus(duree));
        inscriptionRepository.save(enAttente);
    }

    private static String codeDifferentDe(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }
}
