package com.kkooman.lightworkflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiDocumentation() {
        return new OpenAPI()
                .info(new Info()
                        .title("Light Workflow Watchlist API")
                        .description("Watchlist search and index management APIs for AML workflows.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Light Workflow Team")
                                .email("support@example.com")));
    }
}
