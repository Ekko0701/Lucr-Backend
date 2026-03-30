package com.lucr.config;

import com.lucr.config.openapi.OpenApiConstants;
import com.lucr.exception.ErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        return new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme)
                .addResponses(OpenApiConstants.UNAUTHORIZED_RESPONSE_NAME, unauthorizedResponse())
                .addResponses(OpenApiConstants.FORBIDDEN_RESPONSE_NAME, forbiddenResponse())
                .addResponses(OpenApiConstants.VALIDATION_ERROR_RESPONSE_NAME, validationErrorResponse())
                .addResponses(OpenApiConstants.MISSING_PARAMETER_RESPONSE_NAME, missingParameterResponse())
                .addResponses(OpenApiConstants.INVALID_TYPE_RESPONSE_NAME, invalidTypeResponse());
    }

    private ApiResponse unauthorizedResponse() {
        return errorResponse(
                "인증이 필요합니다. (E401004)",
                errorExample(ErrorCode.UNAUTHORIZED_ACCESS)
        );
    }

    private ApiResponse forbiddenResponse() {
        return errorResponse(
                "접근 권한이 없습니다. (E403001)",
                errorExample(ErrorCode.ACCESS_DENIED)
        );
    }

    private ApiResponse validationErrorResponse() {
        Map<String, Object> fieldError = new LinkedHashMap<>();
        fieldError.put("field", "email");
        fieldError.put("value", "abc");
        fieldError.put("reason", "이메일 형식이어야 합니다.");

        return errorResponse(
                "요청 값 검증 실패 (E400001)",
                errorExample(
                        ErrorCode.INVALID_INPUT_VALUE,
                        ErrorCode.INVALID_INPUT_VALUE.getMessage(),
                        List.of(fieldError)
                )
        );
    }

    private ApiResponse missingParameterResponse() {
        return errorResponse(
                "필수 파라미터 누락 (E400003)",
                errorExample(
                        ErrorCode.MISSING_REQUEST_PARAMETER,
                        "'email' 파라미터가 누락되었습니다."
                )
        );
    }

    private ApiResponse invalidTypeResponse() {
        return errorResponse(
                "파라미터 타입 불일치 (E400002)",
                errorExample(
                        ErrorCode.INVALID_TYPE_VALUE,
                        "'status' 파라미터의 타입이 올바르지 않습니다."
                )
        );
    }

    private ApiResponse errorResponse(String description, Map<String, Object> example) {
        MediaType mediaType = new MediaType()
                .schema(new Schema<>().$ref(OpenApiConstants.ERROR_RESPONSE_SCHEMA_REF))
                .example(example);

        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", mediaType));
    }

    private Map<String, Object> errorExample(ErrorCode errorCode) {
        return errorExample(errorCode, errorCode.getMessage(), List.of());
    }

    private Map<String, Object> errorExample(ErrorCode errorCode, String message) {
        return errorExample(errorCode, message, List.of());
    }

    private Map<String, Object> errorExample(
            ErrorCode errorCode,
            String message,
            List<Map<String, Object>> errors
    ) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("code", errorCode.getCode());
        example.put("message", message);
        example.put("status", errorCode.getStatus().value());
        example.put("timestamp", LocalDateTime.of(2026, 3, 30, 12, 34, 56).toString());
        example.put("errors", errors);
        return example;
    }
}
