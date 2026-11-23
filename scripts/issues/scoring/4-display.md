화면에 나가는 숫자가 점수와 맞지 않거나, 같은 개념이 API마다 다른 값으로 나간다.
값의 근거를 정하는 일(상수 근거 이슈)과 달리, 여기서는 현행 값 기준의 정합성만 맞춘다.

## 체크리스트

- [ ] 한계등급이 두 곳에서 다르게 계산된다
  예보는 구름만으로 `6.0 - 구름%/20`, 분석은 Bortle 표에서 가져온다.
  Class 9에 구름 0%면 예보는 "6.0등급", 분석은 "3.0등급"을 표시한다
  → [`StargazingDomainService.java#L46`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/domain/StargazingDomainService.java#L46)
  · [`BortleGrade.java#L12-L20`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/enums/BortleGrade.java#L12-L20)
- [ ] 표시된 감점 합계가 점수와 맞지 않는다
  항목별로 반올림하거나 절삭해 표시하는데, 점수는 마지막에 한 번만 반올림한다.
  습도 65%에 Bortle 4면 점수는 79인데 표시된 감점을 더하면 78이 된다
  → [`StargazingScoringEngine.java#L56-L106`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L56-L106)
- [ ] 태양 고도 -6.0이 세 곳에 하드코딩돼 있고 명칭도 다르다("시민박명" vs "천문학적 황혼")
  → [`StargazingApplicationService.java#L118-L120`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L118-L120)
  · [`RecommendApplicationService.java#L172-L176`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/recommend/application/RecommendApplicationService.java#L172-L176)
- [ ] Gemini의 `final_score`와 등급 텍스트를 파싱해 담지만 아무도 쓰지 않는다
  주석에는 "AI가 계산한 최종 점수 사용"이라 적혀 있어, 누가 쓰는 순간 점수가 LLM 출력으로 바뀐다
  → [`GeminiAnalysisClient.java#L92-L98`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/gemini/GeminiAnalysisClient.java#L92-L98)
- [ ] "완벽한 관측 조건" 분기에 도달할 수 없다
  CSV 최소 Bortle이 2이고 폴백도 4라 광해 감점이 항상 발생한다.
  국내에서 도달 가능한 최고 점수가 94점이라는 사실도 문서에 없다
  → [`StargazingScoringEngine.java#L108-L111`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L108-L111)
- [ ] 달 위상 각도 경계(±10, ±80, ±100, ±170)와 suncalc `getPhase()` 부호 규약을 검증한다
  규약이 틀리면 상현과 하현이 통째로 뒤집히는데, 점수에는 영향이 없어 발견되지 않는다.
  라이브러리 규약을 아직 확인하지 않았다
  → [`MoonPhase.java#L28-L53`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/enums/MoonPhase.java#L28-L53)

## 완료 기준

- 같은 개념이 한 곳에서만 계산되고, 두 API가 같은 입력에 같은 값을 반환한다
- 표시된 감점의 합과 최종 점수가 항상 일치한다
- 도달 불가 분기와 미사용 필드가 정리된다

## 결정 필요

- 한계등급을 무엇의 함수로 정의할지 (광해만 / 광해+구름+달)
- 감점 표시를 정수로 통일할지, 소수점까지 보여줄지
- Gemini에 등급 텍스트 생성을 계속 요구할지 (현재 미사용)

## 관련 문서

[조사 문서](https://github.com/byeolbolil/byeolbolil-backend/blob/dev/docs/issues/scoring.md)

