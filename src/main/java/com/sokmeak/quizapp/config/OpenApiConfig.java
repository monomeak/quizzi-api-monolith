package com.sokmeak.quizapp.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";
    private static final String BEARER_AUTH = "bearerAuth";


    @Bean
    public OpenAPI quizOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Quiz Simple App")
                        .version("v1")
                        .description("""
                                Users register, create quizzes, publish them with a join code,
                                and other users join, answer and appear on the leaderboard.

                                Demo accounts (dev profile only), password 'password123':
                                sokha, dara, vichet - plus admin / admin123.

                                Click Authorize and pick either scheme: paste a token from
                                POST /api/v1/auth/login, or just use a username and password."""))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                // Documentation only - Swagger shows the format, it does not verify it.
                                .bearerFormat("JWT")
                                .description("Paste the accessToken from POST /api/v1/auth/login"))
                        .addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")))
                // Two items rather than one list: either scheme on its own opens an endpoint.
                // One item holding both would mean Swagger sends both at once.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
