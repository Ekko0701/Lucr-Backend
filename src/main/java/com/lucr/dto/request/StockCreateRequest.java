package com.lucr.dto.request;

import com.lucr.entity.Market;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 종목 등록 요청 DTO
 *
 * <h4>요청 예시</h4>
 * <pre>
 * {
 *   "code": "005930",
 *   "name": "삼성전자",
 *   "market": "KOSPI"
 * }
 * </pre>
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCreateRequest {

    @NotBlank(message = "종목코드는 필수입니다.")
    @Size(max = 20, message = "종목코드는 20자 이하여야 합니다.")
    private String code;

    @NotBlank(message = "종목명은 필수입니다.")
    @Size(max = 100, message = "종목명은 100자 이하여야 합니다.")
    private String name;

    @NotNull(message = "시장 구분은 필수입니다.")
    private Market market;
}
