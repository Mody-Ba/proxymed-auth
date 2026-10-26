package com.proxymed.auth.integration;

import com.proxymed.auth.configs.InfosBadge;
import com.proxymed.auth.configs.JwtUtils;
import com.proxymed.auth.entity.Utilisateur;
import com.proxymed.auth.enums.Role;
import com.proxymed.auth.repository.UtilisateurRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Logs du package en DEBUG : on verifie que meme au niveau le plus bavard, aucun mot de
 * passe ni badge n'est ecrit.
 */
@SpringBootTest(properties = "logging.level.com.proxymed.auth=DEBUG")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AuthIntegrationTest {

    private static final String EMAIL_ACTIF = "aminata.diop@proxymed.local";
    private static final String MDP_ACTIF = "Senior#Secret42";
    private static final String EMAIL_INACTIF = "ousmane.ndiaye@proxymed.local";
    private static final String MDP_INACTIF = "Junior#Secret42";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private UUID idActif;

    @BeforeEach
    void setUp() {
        utilisateurRepository.deleteAll();
        idActif = utilisateurRepository.save(compte(EMAIL_ACTIF, MDP_ACTIF, Role.MEDECIN_SENIOR, true)).getId();
        utilisateurRepository.save(compte(EMAIL_INACTIF, MDP_INACTIF, Role.MEDECIN_JUNIOR, false));
    }

    // --- POST /api/auth/connexion ---

    @Test
    void connexion_reussie_renvoieLeBadgeEtLeCompte(CapturedOutput output) throws Exception {
        String corps = connexion(EMAIL_ACTIF, MDP_ACTIF)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.type", is("Bearer")))
                .andExpect(jsonPath("$.expireDansSecondes", is(3600)))
                .andExpect(jsonPath("$.utilisateurId", is(idActif.toString())))
                .andExpect(jsonPath("$.email", is(EMAIL_ACTIF)))
                .andExpect(jsonPath("$.role", is("MEDECIN_SENIOR")))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(corps).get("accessToken").asString();
        InfosBadge badge = jwtUtils.verifierToken(token).orElseThrow();
        assertThat(badge.utilisateurId()).isEqualTo(idActif);
        assertThat(badge.email()).isEqualTo(EMAIL_ACTIF);
        assertThat(badge.roles()).containsExactly("MEDECIN_SENIOR");
        assertThat(entete(token)).contains("\"alg\":\"HS256\"");

        assertThat(corps).doesNotContain(MDP_ACTIF).doesNotContainIgnoringCase("motDePasse");
        assertThat(output.getAll()).doesNotContain(MDP_ACTIF).doesNotContain(token);
    }

    @Test
    void connexion_emailInsensibleALaCasse() throws Exception {
        connexion("Aminata.DIOP@Proxymed.Local", MDP_ACTIF)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(EMAIL_ACTIF)));
    }

    @Test
    void connexion_mauvaisMotDePasse_401MessageGenerique(CapturedOutput output) throws Exception {
        String mauvais = "MauvaisMotDePasse!";
        String corps = connexion(EMAIL_ACTIF, mauvais)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.erreur", is("Authentification echouee")))
                .andExpect(jsonPath("$.message", is("Email ou mot de passe incorrect")))
                .andExpect(jsonPath("$.horodatage", notNullValue()))
                .andExpect(jsonPath("$.details").isArray())
                .andReturn().getResponse().getContentAsString();

        assertThat(corps).doesNotContain(mauvais);
        assertThat(output.getAll()).doesNotContain(mauvais);
    }

    @Test
    void connexion_emailInconnu_memeReponseQueMauvaisMotDePasse() throws Exception {
        JsonNode emailInconnu = objectMapper.readTree(connexion("inconnu@proxymed.local", MDP_ACTIF)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString());
        JsonNode mauvaisMdp = objectMapper.readTree(connexion(EMAIL_ACTIF, "MauvaisMotDePasse!")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString());

        for (String champ : List.of("status", "erreur", "message", "details")) {
            assertThat(emailInconnu.get(champ)).isEqualTo(mauvaisMdp.get(champ));
        }
    }

    @Test
    void connexion_compteInactif_403(CapturedOutput output) throws Exception {
        connexion(EMAIL_INACTIF, MDP_INACTIF)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.erreur", is("Compte inactif")))
                .andExpect(jsonPath("$.message", containsString("desactive")))
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        assertThat(output.getAll()).doesNotContain(MDP_INACTIF);
    }

    @Test
    void connexion_compteInactifMaisMauvaisMotDePasse_401SansReveler() throws Exception {
        connexion(EMAIL_INACTIF, "MauvaisMotDePasse!")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Email ou mot de passe incorrect")));
    }

    @Test
    void connexion_champInconnu_400() throws Exception {
        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL_ACTIF + "\",\"motDePasse\":\"" + MDP_ACTIF + "\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Champ inconnu dans la requete : role")));
    }

    @Test
    void connexion_champsManquants_400AvecDetails() throws Exception {
        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pas-un-email\",\"motDePasse\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Validation echouee")))
                .andExpect(jsonPath("$.details.length()", is(2)));
    }

    // --- GET /api/auth/moi ---

    @Test
    void moi_sansBadge_401Json() throws Exception {
        mockMvc.perform(get("/api/auth/moi"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.erreur", is("Non authentifie")))
                .andExpect(jsonPath("$.message", is("Badge d'authentification manquant")))
                .andExpect(jsonPath("$.horodatage", notNullValue()))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void moi_avecBadge_200() throws Exception {
        String token = badgeValide();

        String corps = mockMvc.perform(get("/api/auth/moi").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(idActif.toString())))
                .andExpect(jsonPath("$.email", is(EMAIL_ACTIF)))
                .andExpect(jsonPath("$.nomComplet", is("Compte de test")))
                .andExpect(jsonPath("$.role", is("MEDECIN_SENIOR")))
                .andExpect(jsonPath("$.actif", is(true)))
                .andExpect(jsonPath("$.dateCreation", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        assertThat(corps).doesNotContainIgnoringCase("motDePasse").doesNotContain("$2a$");
    }

    @Test
    void moi_badgeAvecSignatureModifiee_401() throws Exception {
        String token = badgeValide();
        String[] parties = token.split("\\.");
        // Payload modifie (role ADMIN) en gardant l'ancienne signature.
        String payloadFalsifie = Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(Base64.getUrlDecoder().decode(parties[1]), StandardCharsets.UTF_8)
                        .replace("MEDECIN_SENIOR", "ADMIN").getBytes(StandardCharsets.UTF_8));

        appelerMoiAvec(parties[0] + "." + payloadFalsifie + "." + parties[2]);
    }

    @Test
    void moi_badgeSigneAvecUnAutreSecret_401() throws Exception {
        appelerMoiAvec(Jwts.builder()
                .issuer("proxymed-auth")
                .subject(idActif.toString())
                .claim("email", EMAIL_ACTIF)
                .claim("roles", List.of("ADMIN"))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor("un-autre-secret-de-32-octets-minimum-xx".getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact());
    }

    @Test
    void moi_badgeNonSigne_401() throws Exception {
        String entete = b64("{\"alg\":\"none\"}");
        String payload = b64("{\"iss\":\"proxymed-auth\",\"sub\":\"" + idActif + "\",\"roles\":[\"ADMIN\"]}");
        appelerMoiAvec(entete + "." + payload + ".");
    }

    @Test
    void moi_badgeExpire_401() throws Exception {
        Instant ilYaDeuxHeures = Instant.now().minusSeconds(7200);
        appelerMoiAvec(Jwts.builder()
                .issuer("proxymed-auth")
                .subject(idActif.toString())
                .claim("email", EMAIL_ACTIF)
                .claim("roles", List.of("MEDECIN_SENIOR"))
                .issuedAt(Date.from(ilYaDeuxHeures))
                .expiration(Date.from(ilYaDeuxHeures.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact());
    }

    @Test
    void moi_badgeIllisible_401() throws Exception {
        appelerMoiAvec("ceci-n-est-pas-un-jwt");
    }

    // --- Regles globales ---

    @Test
    void autreAdresse_sansBadge_401() throws Exception {
        mockMvc.perform(get("/api/autre"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void swagger_estPublicEtProposeLeBadgeBearer() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme", is("bearer")));
    }

    // --- Outils ---

    private ResultActions connexion(String email, String motDePasse) throws Exception {
        return mockMvc.perform(post("/api/auth/connexion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new java.util.LinkedHashMap<>(java.util.Map.of(
                        "email", email, "motDePasse", motDePasse)))));
    }

    private String badgeValide() throws Exception {
        String corps = connexion(EMAIL_ACTIF, MDP_ACTIF).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corps).get("accessToken").asString();
    }

    private void appelerMoiAvec(String token) throws Exception {
        mockMvc.perform(get("/api/auth/moi").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Badge invalide ou expire")));
    }

    private Utilisateur compte(String email, String motDePasse, Role role, boolean actif) {
        return Utilisateur.builder()
                .email(email)
                .motDePasse(passwordEncoder.encode(motDePasse))
                .nomComplet("Compte de test")
                .role(role)
                .actif(actif)
                .dateCreation(Instant.now())
                .build();
    }

    private static String entete(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
    }

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
