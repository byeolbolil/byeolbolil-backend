"데이터를 못 가져왔다"와 "실제로 그런 값이다"가 응답에서 구분되지 않는다.
지원 범위(좌표, 시간)도 정의돼 있지 않아 범위 밖 요청이 조용히 엉뚱한 점수를 만든다.
정책 하나를 정해 모든 경로에 같은 규칙으로 적용한다.

## 체크리스트

- [ ] API 실패가 "구름 100%"로 위장되는 경로를 없앤다
  더미 날씨가 하드 컷오프에 걸려 "구름이 하늘을 덮었습니다 (100%)"라는 기상 사유로 0점이 나간다. 맑은 날에도 그렇다
  → [`WeatherDataFactory.java#L14-L21`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/global/util/WeatherDataFactory.java#L14-L21)
  · [`OpenWeatherMapClient.java#L41-L46`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/openweather/OpenWeatherMapClient.java#L41-L46)
- [ ] 예보 실패가 빈 목록 200으로 은폐되는 경로를 정리한다
  → [`StargazingApplicationService.java#L107-L111`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L107-L111)
- [ ] 날씨 응답 null을 방어한다. `getForObject`와 매퍼가 null을 반환할 수 있는데 엔진은 바로 역참조한다
  → [`OpenWeatherResponseMapper.java#L11-L15`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/dto/mapper/OpenWeatherResponseMapper.java#L11-L15)
  · [`StargazingScoringEngine.java#L34-L38`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L34-L38)
- [ ] visibility 결측 대체값을 한 곳으로 모은다. 엔진만 방어하고 표시와 AI 경로는 그대로 언박싱한다
  → [`StargazingApplicationService.java#L78-L82`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L78-L82)
  · [`GeminiAnalysisClient.java#L76`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/gemini/GeminiAnalysisClient.java#L76)
- [ ] 지원 시간 범위를 정의한다
  요청 시각과 예보 항목의 시간 차에 상한이 없어, 과거나 5일 초과 미래도 목록 양 끝 항목으로 점수가 나간다
  → [`WeatherDataProvider.java#L43-L72`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/provider/WeatherDataProvider.java#L43-L72)
  · [`StargazingRequest.java#L28-L36`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/dto/request/StargazingRequest.java#L28-L36)
- [ ] 지원 좌표 범위를 정의한다
  전 세계 좌표를 허용하는데 광해 데이터는 한국뿐이다(lat 33.0~38.996, lon 124.013~130.996).
  범위 밖이면 조용히 Bortle 4로 계산되어 -18.75점이 깎이고, 시각은 항상 Asia/Seoul로 해석된다
  → [`StargazingRequest.java#L16-L26`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/dto/request/StargazingRequest.java#L16-L26)
  · [`StargazingApplicationService.java#L50-L54`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/application/StargazingApplicationService.java#L50-L54)

## 완료 기준

- 실패와 "결과 없음"과 정상값이 응답에서 구분된다
- 지원 범위가 코드 검증과 문서에 함께 반영되고, 범위 밖 요청이 명시적으로 처리된다

## 결정 필요

- 실패 시 응답 형태. 에러로 바꿀지, 0점을 유지하되 "데이터 없음"으로 표기할지
- 서비스 지원 범위. 한국 전용으로 못 박을지, 해외도 허용하되 광해 데이터 없음으로 표시할지
- 지원 시간 범위 (예: 지금부터 +5일)

## 관련 문서

[조사 문서](https://github.com/byeolbolil/byeolbolil-backend/blob/dev/docs/issues/scoring.md)

