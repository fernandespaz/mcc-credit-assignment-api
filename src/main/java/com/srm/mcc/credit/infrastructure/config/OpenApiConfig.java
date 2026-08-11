package com.srm.mcc.credit.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI creditAssignmentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SRM MCC Credit Assignment API")
                        .description("""
                                RESTful API for Credit Assignment (Cessão de Crédito) with:
                                - **Pricing Engine** (Strategy Pattern): Duplicata (1.5% p.m.) and Post-dated Check (2.5% p.m.)
                                - **Currency Engine**: Manual and mocked FX rate management, cross-currency settlement
                                - **ACID Settlements**: Pessimistic locking prevents double-settlement race conditions
                                - **Analytical Reports**: Native SQL settlement statement with period, assignor, and currency filters
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SRM MCC Team")
                                .email("dev@srm-mcc.com"))
                        .license(new License().name("MIT")));
    }
}
