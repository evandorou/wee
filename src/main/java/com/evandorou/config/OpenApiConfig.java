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
                        .description("Winning Events Ensured — event listing and related APIs. User identity is conveyed via `X-User-Id`.")
                        .version("0.1.0"));
    }
}
