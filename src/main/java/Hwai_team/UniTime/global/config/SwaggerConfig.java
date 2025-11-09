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
                                .url("https://<너-Render-도메인-주소>")
                                .description("Render 배포 서버")
                ));
    }

    /**
     * 전체 API 그룹 (원하면 도메인별로 따로 GroupedOpenApi 만들어도 됨)
     */
    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
                .group("전체 API")
                .pathsToMatch("/api/**")
                .build();
    }

    /**
     * Auth 전용 그룹 예시 (Swagger UI에서 탭으로 나눠서 보고 싶으면 활성화)
     */
    @Bean
    public GroupedOpenApi authApis() {
        return GroupedOpenApi.builder()
                .group("Auth API")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    /**
     * Timetable 전용 그룹 예시
     */
    @Bean
    public GroupedOpenApi timetableApis() {
        return GroupedOpenApi.builder()
                .group("Timetable API")
                .pathsToMatch("/api/timetables/**")
                .build();
    }
}