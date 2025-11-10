package Hwai_team.UniTime.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    /**
     * 기본 OpenAPI 메타 정보 설정
     */
    @Bean
    public OpenAPI uniTimeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UniTime API 문서")
                        .description("""
                                UniTime 백엔드 API 명세입니다.
                                - Auth: 회원가입, 로그인, JWT 관련
                                - Timetable: 시간표 조회 및 AI 시간표 생성 (추가 예정)
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("UniTime Team")
                                .email("contact@unitime.app")
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://unitime-be.onrender.com")
                                .description("Render 배포 서버")
                ));
    }

    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
                .group("전체 API")
                .pathsToMatch("/api/**")
                .build();
    }
}