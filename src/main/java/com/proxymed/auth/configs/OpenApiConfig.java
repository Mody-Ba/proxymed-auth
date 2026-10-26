package com.proxymed.auth.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bouton "Authorize" de Swagger : coller l'accessToken recu de /api/auth/connexion
 * (sans le prefixe "Bearer "). Le badge est envoye par defaut sur toutes les operations ;
 * /api/auth/connexion s'en dispense via @SecurityRequirements vide.
 */
@Configuration
public class OpenApiConfig {

    public static final String SCHEMA_BADGE = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PROXYMED Authentification API")
                        .description("Comptes PROXYMED, connexion et delivrance du badge (JWT) "
                                + "verifie par les services Fiche 1 et Fiche 2")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(SCHEMA_BADGE,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEMA_BADGE));
    }
}
