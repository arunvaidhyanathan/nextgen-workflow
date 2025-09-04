package com.flowable.wrapper.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flowable Wrapper V2 API")
                        .version("2.1.0")
                        .description("NextGen Workflow Flowable Wrapper V2 - Core workflow orchestration service using Flowable BPMN engine with authorization bypass configuration, BPMN deployment from files, and real database integration.")
                        .contact(new Contact()
                                .name("NextGen Workflow Team")
                                .email("nextgen-workflow@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}