package com.lucr.dto.response;

import com.lucr.entity.Market;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockResponse {

    private String code;

    private String name;

    private Market market;

    private Integer newsCount;

    private LocalDateTime createdAt;
}
