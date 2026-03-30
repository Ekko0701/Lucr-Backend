package com.lucr.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 에러 응답 DTO
 * 
 * @author kimdongjoo
 * @since 2026-01-28
 */
@Schema(description = "공통 에러 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    @Schema(description = "애플리케이션 에러 코드", example = "E400001")
    private String code;

    @Schema(description = "에러 메시지", example = "입력값이 올바르지 않습니다.")
    private String message;

    @Schema(description = "HTTP 상태 코드", example = "400")
    private int status;
    
    @Schema(description = "에러 발생 시각", example = "2026-03-30T12:34:56")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "필드 검증 오류 목록")
    @Builder.Default
    private List<FieldError> errors = new ArrayList<>();

    /**
     * ErrorCode로부터 ErrorResponse 생성
     */
    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus().value())
                .build();
    }

    /**
     * ErrorCode와 커스텀 메시지로 ErrorResponse 생성
     */
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message)
                .status(errorCode.getStatus().value())
                .build();
    }

    /**
     * 필드 검증 오류 정보
     */
    @Schema(name = "FieldError", description = "필드 단위 검증 오류")
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldError {
        @Schema(description = "오류가 발생한 필드명", example = "email")
        private String field;

        @Schema(description = "잘못 입력된 값", example = "abc")
        private String value;

        @Schema(description = "검증 실패 사유", example = "이메일 형식이어야 합니다.")
        private String reason;

        public static FieldError of(String field, String value, String reason) {
            return FieldError.builder()
                    .field(field)
                    .value(value)
                    .reason(reason)
                    .build();
        }
    }

    /**
     * 필드 오류 추가
     */
    public void addFieldError(String field, String value, String reason) {
        this.errors.add(FieldError.of(field, value, reason));
    }
}
