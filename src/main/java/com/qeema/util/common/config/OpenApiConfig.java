package com.qeema.util.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        private static final String BEARER_AUTH = "bearerAuth";

        @Bean
        OpenAPI qeemaOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Qeema Util API")
                                                .version("v1")
                                                .description("""
                                                                API for identity provisioning, role management,
                                                                and transactional email notifications.

                                                                Administrative endpoints require a Keycloak JWT
                                                                with the ADMIN role.
                                                                """)
                                                .contact(new Contact()
                                                                .name("Qeema Engineering"))
                                                .license(new License()
                                                                .name("Internal use")))
                                .components(new Components()
                                                .addSecuritySchemes(
                                                                BEARER_AUTH,
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")))
                                .addSecurityItem(new SecurityRequirement()
                                                .addList(BEARER_AUTH));
        }
}
