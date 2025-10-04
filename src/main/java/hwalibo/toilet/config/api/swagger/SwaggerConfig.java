package hwalibo.toilet.config.api.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Toilet API",
                version = "v1",
                description = "Toilet 프로젝트 API 문서"
        )
)
public class SwaggerConfig {

        @Bean
        public OpenAPI api() {
                SecurityScheme bearerAuth = new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization");

                // ✅ 전역 보안 요구 제거
                //    .addSecurityItem(securityRequirement) 삭제
                //    각 컨트롤러에서 개별적으로 @SecurityRequirement(name = "bearerAuth") 사용
                return new OpenAPI()
                        .components(new Components().addSecuritySchemes("bearerAuth", bearerAuth));
        }
}
