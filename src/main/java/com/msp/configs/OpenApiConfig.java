package com.msp.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        @Value("${app.public.base-url:http://localhost:5000}")
        private String publicBaseUrl;

        @Bean
        public OpenAPI api() {

                final String schemeName = "Bearer Authentication";

                Server productionServer = new Server();

                productionServer.setUrl(publicBaseUrl.replaceAll("/$", ""));

                productionServer.setDescription("Production Server");

                return new OpenAPI()

                        .info(new Info()
                                .title("MultiTenant SaaS POS Service API")
                                .description("API documentation for MSP")
                                .version("v1"))

                        .servers(List.of(productionServer))

                        .addSecurityItem(
                                new SecurityRequirement().addList(schemeName)
                        )

                        .components(
                                new Components()
                                        .addSecuritySchemes(
                                                schemeName,

                                                new SecurityScheme()
                                                        .name(schemeName)
                                                        .type(SecurityScheme.Type.HTTP)
                                                        .scheme("bearer")
                                                        .bearerFormat("JWT")
                                        )
                        );
        }
}