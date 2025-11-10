package Hwai_team.UniTime.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "UniTime API",
                version = "v1",
                description = "UniTime 백엔드 API 문서"
        )
        // 전체 API에 기본적으로 JWT 필요하게 하고 싶으면 이 줄 유지
        // 특정 API만 잠그고 싶으면 이 줄은 빼고, 각 메서드에 security 달아줘
        , security = { @SecurityRequirement(name = "bearerAuth") }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}