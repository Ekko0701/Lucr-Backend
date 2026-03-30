package com.lucr.config.openapi;

/**
 * OpenAPI 컴포넌트 이름 및 $ref 상수.
 *
 * 컨트롤러에서는 이 상수만 참조하여 공통 에러 응답을 재사용한다.
 */
public final class OpenApiConstants {

    public static final String ERROR_RESPONSE_SCHEMA_NAME = "ErrorResponse";
    public static final String ERROR_RESPONSE_SCHEMA_REF =
            "#/components/schemas/" + ERROR_RESPONSE_SCHEMA_NAME;

    public static final String UNAUTHORIZED_RESPONSE_NAME = "UnauthorizedError";
    public static final String UNAUTHORIZED_RESPONSE_REF =
            "#/components/responses/" + UNAUTHORIZED_RESPONSE_NAME;

    public static final String FORBIDDEN_RESPONSE_NAME = "ForbiddenError";
    public static final String FORBIDDEN_RESPONSE_REF =
            "#/components/responses/" + FORBIDDEN_RESPONSE_NAME;

    public static final String VALIDATION_ERROR_RESPONSE_NAME = "ValidationError";
    public static final String VALIDATION_ERROR_RESPONSE_REF =
            "#/components/responses/" + VALIDATION_ERROR_RESPONSE_NAME;

    public static final String MISSING_PARAMETER_RESPONSE_NAME = "MissingRequestParameterError";
    public static final String MISSING_PARAMETER_RESPONSE_REF =
            "#/components/responses/" + MISSING_PARAMETER_RESPONSE_NAME;

    public static final String INVALID_TYPE_RESPONSE_NAME = "InvalidTypeError";
    public static final String INVALID_TYPE_RESPONSE_REF =
            "#/components/responses/" + INVALID_TYPE_RESPONSE_NAME;

    private OpenApiConstants() {
    }
}
