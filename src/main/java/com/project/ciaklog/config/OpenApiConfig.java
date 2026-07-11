package com.project.ciaklog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Abilita Swagger UI su /swagger-ui.html con il bottone "Authorize" per il JWT.
// Senza questa config, Swagger mostra gli endpoint ma non permette di testare
// quelli protetti (mancherebbe il modo di passare "Authorization: Bearer {token}").
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI ciaklogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CiakLog API")
                        .description("Diario cinematografico personale con AI integrata — documentazione generata automaticamente dal codice")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}