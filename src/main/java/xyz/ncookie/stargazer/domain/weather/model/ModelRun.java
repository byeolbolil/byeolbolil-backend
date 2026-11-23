package xyz.ncookie.stargazer.domain.weather.model;

import java.time.Duration;
import java.time.Instant;

/**
 * 예보 모델의 한 차례 실행.
 *
 * @param initialisedAt  모델이 계산을 시작한 기준 시각. 같은 실행이면 같은 예보다
 * @param updateInterval 다음 실행이 나오는 주기
 */
public record ModelRun(Instant initialisedAt, Duration updateInterval) {}
