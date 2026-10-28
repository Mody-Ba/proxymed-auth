package com.proxymed.auth.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deux origines, avec un espace apres la virgule : verifie aussi le decoupage de PROXYMED_CORS_ORIGINS.
 */
@SpringBootTest(properties = "proxymed.cors.origines=http://localhost:4200, https://front.proxymed.test")
@AutoConfigureMockMvc
class CorsIntegrationTest {

    private static final String ORIGINE_ANGULAR = "http://localhost:4200";
    private static final String AUTRE_ORIGINE_AUTORISEE = "https://front.proxymed.test";
    private static final String ORIGINE_INCONNUE = "http://site-inconnu.test";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preVerification_connexion_origineAutorisee_200AvecEnTetesCors() throws Exception {
        mockMvc.perform(options("/api/auth/connexion")
                        .header(HttpHeaders.ORIGIN, ORIGINE_ANGULAR)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Accept"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGINE_ANGULAR))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Content-Type")))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void preVerification_endpointProtege_sansBadge_pasDe401() throws Exception {
        mockMvc.perform(options("/api/admin/utilisateurs")
                        .header(HttpHeaders.ORIGIN, AUTRE_ORIGINE_AUTORISEE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, AUTRE_ORIGINE_AUTORISEE))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")));
    }

    @Test
    void preVerification_methodeNonAutorisee_403() throws Exception {
        mockMvc.perform(options("/api/auth/connexion")
                        .header(HttpHeaders.ORIGIN, ORIGINE_ANGULAR)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "TRACE"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void preVerification_origineInconnue_refusee() throws Exception {
        mockMvc.perform(options("/api/auth/connexion")
                        .header(HttpHeaders.ORIGIN, ORIGINE_INCONNUE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void requete_origineInconnue_refusee() throws Exception {
        mockMvc.perform(get("/api/auth/moi")
                        .header(HttpHeaders.ORIGIN, ORIGINE_INCONNUE))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    /** Le 401 JSON reste identique, et porte les en-tetes CORS pour que le front puisse le lire. */
    @Test
    void requete_endpointProtege_origineAutorisee_sansBadge_401AvecEnTetesCors() throws Exception {
        mockMvc.perform(get("/api/auth/moi")
                        .header(HttpHeaders.ORIGIN, ORIGINE_ANGULAR))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/json")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGINE_ANGULAR))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, not("*")));
    }
}
