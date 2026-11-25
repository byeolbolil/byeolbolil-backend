package xyz.ncookie.stargazer.domain.recommend.model;

import java.time.Instant;

/**
 * 한 장소에서 본 그 밤의 달. 화면이 달의 모양과 뜨고 지는 시각을 보여 주는 데 쓴다.
 * 등급과 점수에는 쓰지 않는다. 그쪽은 시간 칸마다의 달 고도와 밝기(NightHour)로 계산한다.
 *
 * @param illuminatedFraction 밝게 보이는 면의 비율. 삭 0, 보름 1. 자정의 값이다
 * @param waxing              차오르는 중이면 참, 기우는 중이면 거짓
 * @param rise                그날 정오부터 다음 날 정오 사이에 달이 뜨는 시각. 그 사이에 뜨지 않으면 null
 * @param set                 같은 사이에 달이 지는 시각. 그 사이에 지지 않으면 null
 */
public record NightMoon(double illuminatedFraction, boolean waxing, Instant rise, Instant set) {}
