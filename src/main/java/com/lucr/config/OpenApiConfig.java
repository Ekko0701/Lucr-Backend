package com.lucr.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger) 설정
 *
 * 역할:
 *   1. API 문서 메타데이터 설정 (제목, 설명, 버전, 연락처)
 *   2. JWT Bearer 인증 방식을 Swagger UI에 등록
 *
 * 이 설정이 없으면:
 *   - Swagger UI에 기본 제목("OpenAPI definition")만 표시됨
 *   - "Authorize" 버튼이 없어서 JWT가 필요한 API를 테스트할 수 없음
 *
 * 이 설정이 있으면:
 *   - 커스텀 제목/설명/버전이 표시됨
 *   - "Authorize" 버튼으로 JWT 토큰을 입력하면,
 *     이후 모든 API 호출에 Authorization: Bearer {token} 헤더가 자동 포함됨
 *   - 각 엔드포인트에 자물쇠 아이콘이 표시되어 인증 필요 여부를 시각적으로 확인 가능
 *
 * @author Ekko0701
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    /**
     * OpenAPI 빈 등록
     *
     * springdoc-openapi가 이 빈을 감지하여 자동 생성되는 문서에 설정을 병합합니다.
     * 즉, 컨트롤러 스캔으로 만들어지는 엔드포인트 목록 + 여기서 정의한 메타데이터가 합쳐집니다.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(globalSecurityRequirement())
                .components(securityComponents());
    }

    /**
     * API 메타데이터
     *
     * Swagger UI 상단에 표시되는 정보입니다.
     */
    private Info apiInfo() {
        return new Info()
                .title("Lucr API")
                .description("AI 기반 금융 뉴스 분석 및 투자 추천 플랫폼 API")
                .version("0.0.1-SNAPSHOT")
                .contact(new Contact()
                        .name("Ekko0701")
                        .url("https://github.com/Ekko0701"));
    }

    /**
     * 전역 인증 요구사항
     *
     * 이 설정으로 모든 엔드포인트에 자물쇠 아이콘이 기본 표시됩니다.
     * 인증이 필요 없는 API(로그인, 회원가입 등)는 컨트롤러 메서드에
     * @SecurityRequirements (빈 값)를 붙여서 개별적으로 해제합니다.
     */
    private SecurityRequirement globalSecurityRequirement() {
        return new SecurityRequirement().addList(SECURITY_SCHEME_NAME);
    }

    /**
     * 인증 방식 정의 (JWT Bearer)
     *
     * Components에 SecurityScheme을 등록하면 Swagger UI에 "Authorize" 버튼이 나타납니다.
     *
     * - type: HTTP → HTTP 기반 인증
     * - scheme: bearer → Authorization: Bearer {token} 형식
     * - bearerFormat: JWT → 토큰 형식 힌트 (UI에 표시용, 검증에는 사용 안 됨)
     *
     * 사용 흐름:
     *   1. POST /api/v1/auth/login 호출 → accessToken 획득
     *   2. Swagger UI의 "Authorize" 버튼 클릭
     *   3. 토큰 입력 (Bearer 접두사 없이 토큰만 입력)
     *   4. 이후 모든 API 호출에 헤더가 자동 추가됨
     */
    private Components securityComponents() {
        SecurityScheme securityScheme = new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT 액세스 토큰을 입력하세요. (Bearer 접두사 없이 토큰만 입력)");

        return new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme);
    }
}
