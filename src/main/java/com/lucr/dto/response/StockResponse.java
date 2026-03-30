package com.lucr.dto.response;

import com.lucr.entity.Market;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * 종목 응답 DTO
 *
 * <h4>응답 예시</h4>
 * <pre>
 * {
 *   "code": "005930",
 *   "name": "삼성전자",
 *   "market": "KOSPI",
 *   "newsCount": 5,
 *   "createdAt": "2026-02-12T10:00:00"
 * }
 * </pre>
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
@Schema(description = "종목 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "종목코드", example = "005930")
    private String code;
    @Schema(description = "종목명", example = "삼성전자")
    private String name;
    @Schema(description = "시장 구분", example = "KOSPI")
    private Market market;
    @Schema(description = "관련 뉴스 수", example = "5")
    private Integer newsCount;
    @Schema(description = "등록일", example = "2026-02-12T10:00:00")
    private LocalDateTime createdAt;
}
