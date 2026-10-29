package com.proxymed.auth.integration;

import com.proxymed.auth.configs.JwtUtils;
import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.enums.Role;
import com.proxymed.auth.repository.InscriptionEnAttenteRepository;
import com.proxymed.auth.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MedecinJuniorIntegrationTest {

    private static final String URL = "/api/auth/medecins-juniors";
    private static final String ORIGINE_ANGULAR = "http://localhost:4200";

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

    private Utilisateur admin;
    private Utilisateur senior;
    private Utilisateur patient;
    private Utilisateur juniorZoe;
    private Utilisateur juniorAminata;
    private Utilisateur juniorMoussa;

    @BeforeEach
    void setUp() {
        inscriptionRepository.deleteAll();
        utilisateurRepository.deleteAll();
        admin = compte("admin@proxymed.test", "Admin Principal", Role.ADMIN, true);
        senior = compte("senior@proxymed.test", "Awa Diallo", Role.MEDECIN_SENIOR, true);
        patient = compte("patient@proxymed.test", "Binta Patient", Role.UTILISATEUR, true);
        juniorZoe = compte("zoe@proxymed.test", "Zoe Ndiaye", Role.MEDECIN_JUNIOR, true);
        juniorAminata = compte("aminata@proxymed.test", "Aminata Sow", Role.MEDECIN_JUNIOR, true);
        juniorMoussa = compte("moussa@proxymed.test", "Moussa Ba", Role.MEDECIN_JUNIOR, true);
        compte("desactive@proxymed.test", "Bamba Desactive", Role.MEDECIN_JUNIOR, false);
    }

    @Test
    void senior_200_juniorsActifsTriesParNom() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(senior)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(3)))
                .andExpect(jsonPath("$[*].nomComplet", contains("Aminata Sow", "Moussa Ba", "Zoe Ndiaye")))
                .andExpect(jsonPath("$[*].id", contains(
                        juniorAminata.getId().toString(),
                        juniorMoussa.getId().toString(),
                        juniorZoe.getId().toString())));
    }

    @Test
    void senior_juniorDesactiveSeniorAdminEtUtilisateurAbsents() throws Exception {
        String corps = mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(senior)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corps)
                .doesNotContain("Bamba Desactive")
                .doesNotContain("Awa Diallo")
                .doesNotContain("Admin Principal")
                .doesNotContain("Binta Patient")
                .doesNotContain(senior.getId().toString())
                .doesNotContain(admin.getId().toString())
                .doesNotContain(patient.getId().toString());
    }

    @Test
    void senior_reponseNeContientQueIdEtNomComplet() throws Exception {
        String corps = mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(senior)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].length()", is(2)))
                .andExpect(jsonPath("$[1].length()", is(2)))
                .andExpect(jsonPath("$[2].length()", is(2)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].nomComplet").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(corps)
                .doesNotContain("@proxymed.test")
                .doesNotContain("email")
                .doesNotContain("role")
                .doesNotContain("motDePasse")
                .doesNotContain("actif")
                .doesNotContain("dateCreation")
                .doesNotContain("$2a$");
    }

    @Test
    void juniorUtilisateurEtAdmin_403() throws Exception {
        for (Utilisateur nonSenior : List.of(juniorAminata, patient, admin)) {
            mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(nonSenior)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status", is(403)))
                    .andExpect(jsonPath("$.erreur", is("Acces refuse")));
        }
    }

    @Test
    void sansBadge_401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void cors_preVerificationSansBadge_200AvecEnTetesCors() throws Exception {
        mockMvc.perform(options(URL)
                        .header(HttpHeaders.ORIGIN, ORIGINE_ANGULAR)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGINE_ANGULAR))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")));
    }

    @Test
    void cors_requeteSenior_200AvecEnTetesCors() throws Exception {
        mockMvc.perform(get(URL)
                        .header(HttpHeaders.ORIGIN, ORIGINE_ANGULAR)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senior)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGINE_ANGULAR));
    }

    private String bearer(Utilisateur utilisateur) {
        return "Bearer " + jwtUtils.genererToken(utilisateur.getId(), utilisateur.getEmail(), List.of(utilisateur.getRole().name()));
    }

    private Utilisateur compte(String email, String nomComplet, Role role, boolean actif) {
        return utilisateurRepository.save(Utilisateur.builder()
                .email(email)
                .motDePasse(passwordEncoder.encode("Test#Medecin-2026"))
                .nomComplet(nomComplet)
                .role(role)
                .actif(actif)
                .dateCreation(Instant.now())
                .build());
    }
}
