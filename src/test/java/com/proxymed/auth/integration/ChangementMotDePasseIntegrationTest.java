package com.proxymed.auth.integration;

import com.proxymed.auth.configs.JwtUtils;
import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.enums.Role;
import com.proxymed.auth.repository.InscriptionEnAttenteRepository;
import com.proxymed.auth.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "logging.level.com.proxymed.auth=DEBUG")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ChangementMotDePasseIntegrationTest {

    private static final String URL = "/api/auth/changer-mot-de-passe";
    private static final String EMAIL = "moussa.ba@proxymed.test";
    private static final String TEMPORAIRE = "Tmp#Kx7pQ2wz";
    private static final String NOUVEAU = "Personnel#Secret42";

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

    private Utilisateur medecin;

    @BeforeEach
    void setUp() {
        inscriptionRepository.deleteAll();
        utilisateurRepository.deleteAll();
        medecin = utilisateurRepository.save(Utilisateur.builder()
                .email(EMAIL)
                .motDePasse(passwordEncoder.encode(TEMPORAIRE))
                .nomComplet("Moussa Ba")
                .role(Role.MEDECIN_JUNIOR)
                .actif(true)
                .motDePasseTemporaire(true)
                .dateCreation(Instant.now())
                .build());
    }

    @Test
    void changement_204LeveLeMarqueurTemporaire(CapturedOutput output) throws Exception {
        changer(medecin, TEMPORAIRE, NOUVEAU).andExpect(status().isNoContent());

        Utilisateur apres = utilisateurRepository.findById(medecin.getId()).orElseThrow();
        assertThat(apres.isMotDePasseTemporaire()).isFalse();
        assertThat(passwordEncoder.matches(NOUVEAU, apres.getMotDePasse())).isTrue();

        connexion(TEMPORAIRE).andExpect(status().isUnauthorized());
        connexion(NOUVEAU)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motDePasseTemporaire", is(false)));

        assertThat(output.getAll()).doesNotContain(TEMPORAIRE).doesNotContain(NOUVEAU);
    }

    @Test
    void changement_possiblePourTousLesRoles() throws Exception {
        for (Role role : Role.values()) {
            Utilisateur compte = utilisateurRepository.save(Utilisateur.builder()
                    .email(role.name().toLowerCase() + ".changement@proxymed.test")
                    .motDePasse(passwordEncoder.encode(TEMPORAIRE))
                    .nomComplet("Compte " + role)
                    .role(role)
                    .actif(true)
                    .dateCreation(Instant.now())
                    .build());

            changer(compte, TEMPORAIRE, NOUVEAU).andExpect(status().isNoContent());
        }
    }

    @Test
    void changement_ancienMotDePasseFaux_400() throws Exception {
        changer(medecin, "Mauvais#Ancien1", NOUVEAU)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("L'ancien mot de passe est incorrect")));

        assertThat(utilisateurRepository.findById(medecin.getId()).orElseThrow().isMotDePasseTemporaire()).isTrue();
    }

    @Test
    void changement_nouveauIdentiqueALAncien_400() throws Exception {
        changer(medecin, TEMPORAIRE, TEMPORAIRE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Le nouveau mot de passe doit etre different de l'ancien")));
    }

    @Test
    void changement_nouveauTropCourtOuTropLong_400() throws Exception {
        changer(medecin, TEMPORAIRE, "Court#1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("entre 8 et 72 caracteres")));
        changer(medecin, TEMPORAIRE, "x".repeat(73)).andExpect(status().isBadRequest());
    }

    @Test
    void changement_sansBadge_401() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ancienMotDePasse", TEMPORAIRE, "nouveauMotDePasse", NOUVEAU))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changement_compteDesactive_403() throws Exception {
        medecin.setActif(false);
        utilisateurRepository.save(medecin);

        changer(medecin, TEMPORAIRE, NOUVEAU)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erreur", is("Compte inactif")));
    }

    private ResultActions changer(Utilisateur compte, String ancien, String nouveau) throws Exception {
        String badge = jwtUtils.genererToken(compte.getId(), compte.getEmail(), List.of(compte.getRole().name()));
        return mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + badge)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("ancienMotDePasse", ancien, "nouveauMotDePasse", nouveau))));
    }

    private ResultActions connexion(String motDePasse) throws Exception {
        return mockMvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", EMAIL, "motDePasse", motDePasse))));
    }

    private String json(Map<String, String> champs) {
        return objectMapper.writeValueAsString(new LinkedHashMap<>(champs));
    }
}
