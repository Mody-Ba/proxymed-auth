package com.proxymed.auth.security;

import com.proxymed.auth.configs.security.AccesRefuseHandler;
import com.proxymed.auth.configs.security.ReponseErreurJson;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aucune adresse n'est encore reservee a un role (etapes suivantes) : le 403 de Spring Security
 * est verifie directement sur le handler.
 */
class AccesRefuseHandlerTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void accesRefuse_403AuFormatErrorResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AccesRefuseHandler(new ReponseErreurJson(jsonMapper))
                .handle(new MockHttpServletRequest(), response, new AccessDeniedException("refuse"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode corps = jsonMapper.readTree(response.getContentAsString());
        assertThat(corps.get("status").asInt()).isEqualTo(403);
        assertThat(corps.get("erreur").asString()).isEqualTo("Acces refuse");
        assertThat(corps.get("message").asString()).isNotBlank();
        assertThat(corps.get("horodatage").isNull()).isFalse();
        assertThat(corps.get("details").isArray()).isTrue();
    }
}
