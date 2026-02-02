package com.lucr.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 단위 테스트
 * 
 * @author kimdongjoo
 * @since 2026-01-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("BusinessException 처리")
    class HandleBusinessException {

        @Test
        @DisplayName("ResourceNotFoundException 처리 - 404 NOT_FOUND 반환")
        void handleResourceNotFoundException_returnsNotFound() {
            // given
            UUID newsId = UUID.randomUUID();
            ResourceNotFoundException exception = ResourceNotFoundException.newsNotFound(newsId.toString());

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.NEWS_NOT_FOUND.getCode());
            assertThat(response.getBody().getMessage()).contains(newsId.toString());
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("DuplicateResourceException 처리 - 409 CONFLICT 반환")
        void handleDuplicateResourceException_returnsConflict() {
            // given
            String url = "https://example.com/news";
            DuplicateResourceException exception = DuplicateResourceException.duplicateNewsUrl(url);

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.DUPLICATE_NEWS_URL.getCode());
            assertThat(response.getBody().getMessage()).contains(url);
            assertThat(response.getBody().getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("BusinessException 일반 케이스 처리")
        void handleBusinessException_returnsCorrectStatusAndMessage() {
            // given
            BusinessException exception = new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR) {};

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
            assertThat(response.getBody().getStatus()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException 처리")
    class HandleMethodArgumentNotValidException {

        @Test
        @DisplayName("@Valid 검증 실패 - 400 BAD_REQUEST와 FieldError 리스트 반환")
        void handleMethodArgumentNotValidException_returnsBadRequestWithFieldErrors() {
            // given
            BindingResult bindingResult = mock(BindingResult.class);
            
            FieldError fieldError1 = new FieldError("newsCreateRequest", "title", "", false, null, null, "제목은 필수입니다");
            FieldError fieldError2 = new FieldError("newsCreateRequest", "url", "invalid-url", false, null, null, "올바른 URL 형식이 아닙니다");

            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
            
            BindException bindException = new BindException(bindingResult);

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBindException(bindException);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getErrors()).hasSize(2);
            assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("title");
            assertThat(response.getBody().getErrors().get(0).getValue()).isEqualTo("");
            assertThat(response.getBody().getErrors().get(0).getReason()).isEqualTo("제목은 필수입니다");
            assertThat(response.getBody().getErrors().get(1).getField()).isEqualTo("url");
            assertThat(response.getBody().getErrors().get(1).getValue()).isEqualTo("invalid-url");
        }

        @Test
        @DisplayName("null 값으로 인한 검증 실패 - value를 빈 문자열로 처리")
        void handleMethodArgumentNotValidException_handlesNullValueAsEmptyString() {
            // given
            BindingResult bindingResult = mock(BindingResult.class);

            FieldError fieldError = new FieldError("newsCreateRequest", "content", null, false, null, null, "내용은 필수입니다");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
            
            BindException bindException = new BindException(bindingResult);

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBindException(bindException);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrors()).hasSize(1);
            assertThat(response.getBody().getErrors().get(0).getValue()).isEqualTo("");
        }

        @Test
        @DisplayName("여러 필드 검증 실패 - 모든 FieldError 포함")
        void handleMethodArgumentNotValidException_includesAllFieldErrors() {
            // given
            BindingResult bindingResult = mock(BindingResult.class);

            List<FieldError> fieldErrors = List.of(
                    new FieldError("request", "title", "", false, null, null, "제목 필수"),
                    new FieldError("request", "url", "bad", false, null, null, "URL 형식 오류"),
                    new FieldError("request", "content", "short", false, null, null, "내용 너무 짧음"),
                    new FieldError("request", "source", null, false, null, null, "출처 필수")
            );
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
            
            BindException bindException = new BindException(bindingResult);

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBindException(bindException);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrors()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("BindException 처리")
    class HandleBindException {

        @Test
        @DisplayName("@ModelAttribute 검증 실패 - 400 BAD_REQUEST와 FieldError 리스트 반환")
        void handleBindException_returnsBadRequestWithFieldErrors() {
            // given
            BindException exception = new BindException(new Object(), "searchRequest");
            exception.addError(new FieldError("searchRequest", "page", "-1", false, null, null, "페이지는 0 이상이어야 합니다"));
            exception.addError(new FieldError("searchRequest", "size", "1000", false, null, null, "페이지 크기는 최대 100입니다"));

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBindException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getErrors()).hasSize(2);
            assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("page");
            assertThat(response.getBody().getErrors().get(0).getValue()).isEqualTo("-1");
        }

        @Test
        @DisplayName("빈 FieldError 리스트 - ErrorResponse에 빈 errors 포함")
        void handleBindException_withNoFieldErrors_returnsEmptyErrorsList() {
            // given
            BindException exception = new BindException(new Object(), "emptyRequest");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBindException(exception);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("MethodArgumentTypeMismatchException 처리")
    class HandleMethodArgumentTypeMismatchException {

        @Test
        @DisplayName("타입 불일치 - 400 BAD_REQUEST와 동적 메시지 반환")
        void handleMethodArgumentTypeMismatchException_returnsBadRequestWithDynamicMessage() {
            // given
            MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                    "abc", UUID.class, "id", null, null
            );

            // when
            ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatchException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_TYPE_VALUE.getCode());
            assertThat(response.getBody().getMessage()).contains("id");
            assertThat(response.getBody().getMessage()).contains("파라미터의 타입이 올바르지 않습니다");
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("숫자 타입 불일치 - 파라미터명이 메시지에 포함")
        void handleMethodArgumentTypeMismatchException_includesParameterNameInMessage() {
            // given
            MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                    "notANumber", Integer.class, "pageSize", null, null
            );

            // when
            ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatchException(exception);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("'pageSize' 파라미터의 타입이 올바르지 않습니다.");
        }
    }

    @Nested
    @DisplayName("MissingServletRequestParameterException 처리")
    class HandleMissingServletRequestParameterException {

        @Test
        @DisplayName("필수 파라미터 누락 - 400 BAD_REQUEST와 동적 메시지 반환")
        void handleMissingServletRequestParameterException_returnsBadRequestWithDynamicMessage() {
            // given
            MissingServletRequestParameterException exception = new MissingServletRequestParameterException(
                    "source", "String"
            );

            // when
            ResponseEntity<ErrorResponse> response = handler.handleMissingServletRequestParameterException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.MISSING_REQUEST_PARAMETER.getCode());
            assertThat(response.getBody().getMessage()).contains("source");
            assertThat(response.getBody().getMessage()).contains("파라미터가 누락되었습니다");
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("다른 파라미터 누락 - 파라미터명이 메시지에 포함")
        void handleMissingServletRequestParameterException_includesParameterNameInMessage() {
            // given
            MissingServletRequestParameterException exception = new MissingServletRequestParameterException(
                    "query", "String"
            );

            // when
            ResponseEntity<ErrorResponse> response = handler.handleMissingServletRequestParameterException(exception);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("'query' 파라미터가 누락되었습니다.");
        }
    }

    @Nested
    @DisplayName("Exception 처리 (일반 예외)")
    class HandleException {

        @Test
        @DisplayName("일반 예외 - 500 INTERNAL_SERVER_ERROR 반환")
        void handleException_returnsInternalServerError() {
            // given
            Exception exception = new RuntimeException("예상치 못한 오류 발생");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
            assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
            assertThat(response.getBody().getStatus()).isEqualTo(500);
        }

        @Test
        @DisplayName("NullPointerException - 500 INTERNAL_SERVER_ERROR 반환")
        void handleException_withNullPointerException_returnsInternalServerError() {
            // given
            Exception exception = new NullPointerException("Null 참조 오류");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        }

        @Test
        @DisplayName("IllegalArgumentException - 500 INTERNAL_SERVER_ERROR 반환")
        void handleException_withIllegalArgumentException_returnsInternalServerError() {
            // given
            Exception exception = new IllegalArgumentException("잘못된 인자");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        }
    }

    @Nested
    @DisplayName("ErrorResponse 포맷 검증")
    class ErrorResponseFormat {

        @Test
        @DisplayName("모든 응답에 timestamp 포함")
        void allResponses_includeTimestamp() {
            // given
            ResourceNotFoundException exception = ResourceNotFoundException.newsNotFound(UUID.randomUUID().toString());

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("모든 응답에 code, message, status 포함")
        void allResponses_includeRequiredFields() {
            // given
            Exception exception = new RuntimeException("테스트");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleException(exception);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isNotBlank();
            assertThat(response.getBody().getMessage()).isNotBlank();
            assertThat(response.getBody().getStatus()).isPositive();
        }

        @Test
        @DisplayName("Validation 오류가 아닌 경우 errors 리스트는 비어있음")
        void nonValidationErrors_haveEmptyErrorsList() {
            // given
            ResourceNotFoundException exception = ResourceNotFoundException.newsNotFound(UUID.randomUUID().toString());

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getErrors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("HTTP Status Code 검증")
    class HttpStatusCodeValidation {

        @Test
        @DisplayName("NOT_FOUND 예외 - 404 반환")
        void notFoundException_returns404() {
            // given
            ResourceNotFoundException exception = ResourceNotFoundException.newsNotFound("test");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("CONFLICT 예외 - 409 반환")
        void conflictException_returns409() {
            // given
            DuplicateResourceException exception = DuplicateResourceException.duplicateNewsUrl("test");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("BAD_REQUEST 예외 - 400 반환")
        void badRequestException_returns400() {
            // given
            MissingServletRequestParameterException exception = new MissingServletRequestParameterException("test", "String");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleMissingServletRequestParameterException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("INTERNAL_SERVER_ERROR 예외 - 500 반환")
        void internalServerErrorException_returns500() {
            // given
            Exception exception = new RuntimeException("test");

            // when
            ResponseEntity<ErrorResponse> response = handler.handleException(exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
        }
    }
}
