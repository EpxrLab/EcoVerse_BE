package com.sep490.ecoverse_be.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    private static final String SECURITY_SCHEMES = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EcoVerse API")
                        .version("1.0")
                        .description("API for EcoVerse"))
                .addServersItem(new Server()
                        .url("https://api.ecoverse-system.io.vn")
                        .description("Production Server"))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Development Server"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEMES))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEMES,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEMES)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}

