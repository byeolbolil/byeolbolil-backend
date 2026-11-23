테스트가 컨텍스트 로드 하나뿐이라, 점수 상수를 손댔을 때 무엇이 달라졌는지 알 방법이 없다.
값은 그대로 두고 현행 동작만 고정하는 특성화 테스트를 먼저 둔다.

## 체크리스트

- [ ] 태양 고도 경계. 정확히 -6.0°는 밤으로 판정되는 현행 동작을 고정한다
  → [`StargazingScoringEngine.java#L40-L44`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L40-L44) (`calculateScore`의 `sunPos.getAltitude() > -6.0`)
- [ ] 구름 경계. 70%는 0점, 69%는 41점, 10%는 무감점
  → [`StargazingScoringEngine.java#L46-L63`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L46-L63)
- [ ] 습도 경계. 60%는 무감점, 65%는 -2.5점
  → [`StargazingScoringEngine.java#L66-L72`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L66-L72)
- [ ] 시정 경계. 정확히 10000m는 무감점, 5000m는 -10점
  → [`StargazingScoringEngine.java#L74-L83`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L74-L83) (`VISIBILITY_THRESHOLD_GOOD` / `VISIBILITY_THRESHOLD_BAD`)
- [ ] 달 경계. 고도 정확히 0°는 무감점, 감점이 1점 미만이면 사유도 생략된다
  → [`StargazingScoringEngine.java#L85-L94`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L85-L94)
- [ ] 광해. Bortle 2~9 각각의 감점값
  → [`StargazingScoringEngine.java#L96-L103`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L96-L103)
- [ ] 감점 총합이 100을 넘을 때 0으로 클램프되는 동작
  → [`StargazingScoringEngine.java#L105-L106`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L105-L106)

## 완료 기준

위 경계값 전부에 대해 현행 결과를 그대로 통과시키는 테스트가 있다.
이 테스트가 초록인 상태에서만 다른 이슈에 착수한다.

## 결정 필요

없음

## 관련 문서

경계값 판정 결과는 [조사 문서](https://github.com/byeolbolil/byeolbolil-backend/blob/dev/docs/issues/scoring.md)의 2단계 표에 정리돼 있다.

