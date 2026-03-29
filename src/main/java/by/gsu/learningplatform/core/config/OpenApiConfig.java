package by.gsu.learningplatform.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String bearerAuthScheme = "bearerAuth";

    @Bean
    public OpenAPI learningPlatformOpenApi() {
        final var bearerScheme = new SecurityScheme()
                .name(bearerAuthScheme)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER);

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(bearerAuthScheme, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(bearerAuthScheme))
                .info(new Info()
                        .title("Learning Platform API")
                        .description("Production backend API for the learning platform")
                        .version("1.0.0")
                        .contact(new Contact().name("Learning Platform Team"))
                        .license(new License().name("Proprietary")));
    }
}
