package com.proxymed.auth.security;

import com.proxymed.auth.configs.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preuve que les droits se verifient dans les controllers avec @PreAuthorize, a partir du role
 * lu dans le badge. Le controller ci-dessous n'existe que pour ce test : classe imbriquee dans
 * une classe de test, Spring Boot l'exclut du scan des autres tests, et elle n'est pas dans
 * le code de production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PreAuthorizeRoleTest.ControleurReserveAdmin.class)
class PreAuthorizeRoleTest {

    private static final String URL = "/api/test/reserve-admin";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtils jwtUtils;

    @RestController
    static class ControleurReserveAdmin {

        @GetMapping(URL)
        @PreAuthorize("hasRole('ADMIN')")
        public String reserveAdmin() {
            return "ok";
        }
    }

    @Test
    void sansBadge_401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void roleUtilisateur_403() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + badge("UTILISATEUR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.erreur", is("Acces refuse")))
                .andExpect(jsonPath("$.horodatage").exists())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void roleMedecinSenior_403() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + badge("MEDECIN_SENIOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleAdmin_200() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + badge("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    private String badge(String role) {
        return jwtUtils.genererToken(UUID.randomUUID(), role.toLowerCase() + "@proxymed.local", List.of(role));
    }
}
