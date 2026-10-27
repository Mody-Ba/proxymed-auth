package com.proxymed.auth.integration;

import com.proxymed.auth.configs.JwtUtils;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EmailService est remplace par un mock : aucun email n'est envoye. Le mot de passe temporaire
 * est recupere sur le mock, comme le medecin le lirait dans sa boite mail.
 */
@SpringBootTest(properties = "logging.level.com.proxymed.auth=DEBUG")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AdminUtilisateurIntegrationTest {

    private static final String URL = "/api/admin/utilisateurs";
    private static final String EMAIL_MEDECIN = "moussa.ba@proxymed.test";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private InscriptionEnAttenteRepository inscriptionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    private Utilisateur admin;
    private Utilisateur senior;
    private Utilisateur patient;

    @BeforeEach
    void setUp() {
        inscriptionRepository.deleteAll();
        utilisateurRepository.deleteAll();
        admin = compte("admin@proxymed.test", Role.ADMIN, "Admin#Test-2026");
        senior = compte("senior@proxymed.test", Role.MEDECIN_SENIOR, "Senior#Test-2026");
        patient = compte("patient@proxymed.test", Role.UTILISATEUR, "Patient#Test-2026");
    }

    // --- Securite : reserve a ADMIN ---

    @Test
    void endpointsAdmin_sansBadge_401() throws Exception {
        for (MockHttpServletRequestBuilder requete : requetesAdmin()) {
            mockMvc.perform(requete)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)));
        }
    }

    @Test
    void endpointsAdmin_utilisateurEtMedecins_403() throws Exception {
        Utilisateur junior = compte("junior@proxymed.test", Role.MEDECIN_JUNIOR, "Junior#Test-2026");
        for (Utilisateur nonAdmin : List.of(patient, senior, junior)) {
            for (MockHttpServletRequestBuilder requete : requetesAdmin()) {
                mockMvc.perform(requete.header(HttpHeaders.AUTHORIZATION, bearer(nonAdmin)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.status", is(403)))
                        .andExpect(jsonPath("$.erreur", is("Acces refuse")));
            }
        }
        // Aucun effet de bord : rien cree, rien desactive, aucun email.
        assertThat(utilisateurRepository.count()).isEqualTo(4);
        assertThat(utilisateurRepository.findById(senior.getId()).orElseThrow().isActif()).isTrue();
        verify(emailService, never()).envoyerMotDePasseTemporaire(any(), any(), any());
    }

    // --- POST /api/admin/utilisateurs ---

    @Test
    void creer_medecinJunior_201AvecMotDePasseTemporaireEnvoyeParEmail(CapturedOutput output) throws Exception {
        String corps = creer("Moussa Ba", EMAIL_MEDECIN, "MEDECIN_JUNIOR")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is(EMAIL_MEDECIN)))
                .andExpect(jsonPath("$.nomComplet", is("Moussa Ba")))
                .andExpect(jsonPath("$.role", is("MEDECIN_JUNIOR")))
                .andExpect(jsonPath("$.actif", is(true)))
                .andExpect(jsonPath("$.motDePasseTemporaire", is(true)))
                .andReturn().getResponse().getContentAsString();

        String motDePasse = dernierMotDePasseEnvoye();
        assertThat(motDePasse).hasSize(12);
        verify(emailService).envoyerMotDePasseTemporaire(eq(EMAIL_MEDECIN), eq("Moussa Ba"), eq(motDePasse));

        Utilisateur medecin = utilisateurRepository.findByEmail(EMAIL_MEDECIN).orElseThrow();
        assertThat(medecin.getMotDePasse()).startsWith("$2a$").isNotEqualTo(motDePasse);
        assertThat(passwordEncoder.matches(motDePasse, medecin.getMotDePasse())).isTrue();
        assertThat(medecin.isMotDePasseTemporaire()).isTrue();

        assertThat(corps).doesNotContain(motDePasse).doesNotContain("\"motDePasse\"");
        assertThat(output.getAll()).doesNotContain(motDePasse);

        // Le medecin se connecte avec le mot de passe recu ; la connexion signale qu'il est temporaire.
        connexion(EMAIL_MEDECIN, motDePasse)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("MEDECIN_JUNIOR")))
                .andExpect(jsonPath("$.motDePasseTemporaire", is(true)));
    }

    @Test
    void creer_medecinSenior_201() throws Exception {
        creer("Awa Diallo", "Awa.Diallo@Proxymed.Test", "MEDECIN_SENIOR")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("MEDECIN_SENIOR")))
                .andExpect(jsonPath("$.email", is("awa.diallo@proxymed.test")));
    }

    @Test
    void creer_roleAdminOuUtilisateur_400() throws Exception {
        for (String role : List.of("ADMIN", "UTILISATEUR")) {
            creer("Pas Medecin", "pas.medecin@proxymed.test", role)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erreur", is("Regle de gestion non respectee")))
                    .andExpect(jsonPath("$.message", containsString("MEDECIN_JUNIOR ou MEDECIN_SENIOR")));
        }
        assertThat(utilisateurRepository.findByEmail("pas.medecin@proxymed.test")).isEmpty();
        verify(emailService, never()).envoyerMotDePasseTemporaire(any(), any(), any());
    }

    @Test
    void creer_roleInconnuOuAbsent_400() throws Exception {
        creer("Moussa Ba", EMAIL_MEDECIN, "CHIRURGIEN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Valeur invalide pour le champ role")));

        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nomComplet", "Moussa Ba", "email", EMAIL_MEDECIN))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("le role est obligatoire")));
    }

    @Test
    void creer_champsInvalidesOuInconnus_400() throws Exception {
        creer("", "pas-un-email", "MEDECIN_JUNIOR")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()", is(2)));

        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nomComplet", "Moussa Ba", "email", EMAIL_MEDECIN,
                                "role", "MEDECIN_JUNIOR", "motDePasse", "Choisi#Par-Admin1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Champ inconnu dans la requete : motDePasse")));
    }

    @Test
    void creer_emailDejaUtilise_409() throws Exception {
        creer("Autre Nom", "SENIOR@proxymed.test", "MEDECIN_JUNIOR")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Un compte existe deja avec cet email")));
        verify(emailService, never()).envoyerMotDePasseTemporaire(any(), any(), any());
    }

    @Test
    void creer_echecDEnvoiDeLEmail_503EtRienNEstEnregistre() throws Exception {
        doThrow(new EnvoiEmailException(new RuntimeException("SMTP indisponible")))
                .when(emailService).envoyerMotDePasseTemporaire(anyString(), anyString(), anyString());

        creer("Moussa Ba", EMAIL_MEDECIN, "MEDECIN_JUNIOR")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", is(503)));

        assertThat(utilisateurRepository.findByEmail(EMAIL_MEDECIN)).isEmpty();
    }

    // --- GET /api/admin/utilisateurs ---

    @Test
    void lister_pagine() throws Exception {
        mockMvc.perform(get(URL).param("page", "0").param("taille", "2").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu.length()", is(2)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.taille", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.contenu[0].motDePasse").doesNotExist());

        mockMvc.perform(get(URL).param("page", "1").param("taille", "2").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu.length()", is(1)));
    }

    @Test
    void lister_filtreParRole() throws Exception {
        compte("senior2@proxymed.test", Role.MEDECIN_SENIOR, "Senior2#Test-2026");

        mockMvc.perform(get(URL).param("role", "MEDECIN_SENIOR").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.contenu[*].role", everyItem(is("MEDECIN_SENIOR"))));
    }

    @Test
    void lister_parametresInvalides_400() throws Exception {
        mockMvc.perform(get(URL).param("role", "CHIRURGIEN").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Valeur invalide pour le parametre role")));
        mockMvc.perform(get(URL).param("taille", "500").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("taille ne doit pas depasser 100")));
    }

    // --- GET /api/admin/utilisateurs/{id} ---

    @Test
    void detail_200() throws Exception {
        mockMvc.perform(get(URL + "/" + senior.getId()).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(senior.getId().toString())))
                .andExpect(jsonPath("$.email", is("senior@proxymed.test")))
                .andExpect(jsonPath("$.role", is("MEDECIN_SENIOR")));
    }

    @Test
    void detail_compteAbsent_404_idInvalide_400() throws Exception {
        mockMvc.perform(get(URL + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Compte introuvable")));
        mockMvc.perform(get(URL + "/pas-un-uuid").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Valeur invalide pour le parametre id")));
    }

    // --- PATCH /api/admin/utilisateurs/{id}/activation ---

    @Test
    void activation_compteDesactiveNePeutPlusSeConnecterPuisReactive() throws Exception {
        activation(senior.getId(), false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif", is(false)));
        connexion("senior@proxymed.test", "Senior#Test-2026")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erreur", is("Compte inactif")));

        activation(senior.getId(), true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif", is(true)));
        connexion("senior@proxymed.test", "Senior#Test-2026").andExpect(status().isOk());
    }

    @Test
    void activation_adminNePeutPasSeDesactiverLuiMeme_400() throws Exception {
        activation(admin.getId(), false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Un administrateur ne peut pas desactiver son propre compte")));
        assertThat(utilisateurRepository.findById(admin.getId()).orElseThrow().isActif()).isTrue();
    }

    @Test
    void activation_actifAbsent_400_compteAbsent_404() throws Exception {
        mockMvc.perform(patch(URL + "/" + senior.getId() + "/activation").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("actif est obligatoire")));
        activation(UUID.randomUUID(), false).andExpect(status().isNotFound());
    }

    // --- POST /api/admin/utilisateurs/{id}/reinitialiser-mot-de-passe ---

    @Test
    void reinitialiser_nouveauMotDePasseTemporaireEnvoyeParEmail(CapturedOutput output) throws Exception {
        String corps = reinitialiser(senior.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motDePasseTemporaire", is(true)))
                .andReturn().getResponse().getContentAsString();

        String motDePasse = dernierMotDePasseEnvoye();
        verify(emailService).envoyerMotDePasseTemporaire(eq("senior@proxymed.test"), any(), eq(motDePasse));
        assertThat(corps).doesNotContain(motDePasse);
        assertThat(output.getAll()).doesNotContain(motDePasse);

        connexion("senior@proxymed.test", "Senior#Test-2026").andExpect(status().isUnauthorized());
        connexion("senior@proxymed.test", motDePasse)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motDePasseTemporaire", is(true)));
    }

    @Test
    void reinitialiser_echecDEnvoiDeLEmail_503EtAncienMotDePasseConserve() throws Exception {
        doThrow(new EnvoiEmailException(new RuntimeException("SMTP indisponible")))
                .when(emailService).envoyerMotDePasseTemporaire(anyString(), anyString(), anyString());

        reinitialiser(senior.getId()).andExpect(status().isServiceUnavailable());

        connexion("senior@proxymed.test", "Senior#Test-2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motDePasseTemporaire", is(false)));
    }

    @Test
    void reinitialiser_compteAbsent_404() throws Exception {
        reinitialiser(UUID.randomUUID()).andExpect(status().isNotFound());
    }

    // --- Outils ---

    /** Une requete par endpoint admin, avec un corps valide. */
    private List<MockHttpServletRequestBuilder> requetesAdmin() {
        return List.of(
                post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nomComplet", "Moussa Ba", "email", EMAIL_MEDECIN, "role", "MEDECIN_JUNIOR"))),
                get(URL),
                get(URL + "/" + senior.getId()),
                patch(URL + "/" + senior.getId() + "/activation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actif\":false}"),
                post(URL + "/" + senior.getId() + "/reinitialiser-mot-de-passe"));
    }

    private ResultActions creer(String nomComplet, String email, String role) throws Exception {
        return mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nomComplet", nomComplet, "email", email, "role", role))));
    }

    private ResultActions activation(UUID id, boolean actif) throws Exception {
        return mockMvc.perform(patch(URL + "/" + id + "/activation").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"actif\":" + actif + "}"));
    }

    private ResultActions reinitialiser(UUID id) throws Exception {
        return mockMvc.perform(post(URL + "/" + id + "/reinitialiser-mot-de-passe")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin)));
    }

    private ResultActions connexion(String email, String motDePasse) throws Exception {
        return mockMvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "motDePasse", motDePasse))));
    }

    private String dernierMotDePasseEnvoye() {
        ArgumentCaptor<String> motDePasse = ArgumentCaptor.forClass(String.class);
        verify(emailService).envoyerMotDePasseTemporaire(anyString(), anyString(), motDePasse.capture());
        return motDePasse.getValue();
    }

    private String bearer(Utilisateur utilisateur) {
        return "Bearer " + jwtUtils.genererToken(utilisateur.getId(), utilisateur.getEmail(), List.of(utilisateur.getRole().name()));
    }

    private String json(Map<String, String> champs) {
        return objectMapper.writeValueAsString(new LinkedHashMap<>(champs));
    }

    private Utilisateur compte(String email, Role role, String motDePasse) {
        return utilisateurRepository.save(Utilisateur.builder()
                .email(email)
                .motDePasse(passwordEncoder.encode(motDePasse))
                .nomComplet("Compte de test")
                .role(role)
                .actif(true)
                .dateCreation(Instant.now())
                .build());
    }
}
