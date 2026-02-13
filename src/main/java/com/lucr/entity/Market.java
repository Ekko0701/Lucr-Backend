package com.lucr.entity;

/**
 * 주식 시장 구분 enum
 *
 * <h3>한국 시장</h3>
 * <ul>
 *   <li>KOSPI: 유가증권시장 (대형주 중심)</li>
 *   <li>KOSDAQ: 코스닥시장 (중소·벤처 중심)</li>
 * </ul>
 *
 * <h3>미국 시장</h3>
 * <ul>
 *   <li>NYSE: 뉴욕증권거래소 (New York Stock Exchange)</li>
 *   <li>NASDAQ: 나스닥 (기술주 중심)</li>
 *   <li>AMEX: 아메리칸증권거래소 (소형주, ETF 중심)</li>
 * </ul>
 *
 * @author Ekko0701
 * @since 2026-02-12
 */
public enum Market {

    // 한국 시장
    KOSPI,
    KOSDAQ,

    // 미국 시장
    NYSE,
    NASDAQ,
    AMEX
}
