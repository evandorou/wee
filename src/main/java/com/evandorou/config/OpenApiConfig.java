package com.evandorou.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI weeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("WEE API")
                        .description("""
                                Winning Events Ensured — event listing, OpenF1-backed bet placement, and settlement.
                                Request and response bodies use **JSON** with **camelCase** property names unless noted.
                                User identity is conveyed via `X-User-Id` (external id). Internal `wee_user.id` (UUID) and
                                EUR balance live only in PostgreSQL; balance changes through bet placement/settlement, not a public top-up API.
                                Field-level descriptions match the Postman collection notes in `postman/README.md`.""")
                        .version("0.1.0"));
    }
}
