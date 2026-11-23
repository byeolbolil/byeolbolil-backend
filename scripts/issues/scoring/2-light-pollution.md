광해 등급은 저장소 밖 Python 스크립트가 계산한 CSV 값을 그대로 신뢰하는 구조다.
등급 기준을 바꾸려면 11GB 원본 래스터부터 다시 처리해야 한다.

원측정값 radiance는 이미 DB에 적재돼 있고 아무도 읽지 않는다(`getRadiance()` 호출부 0건).
따라서 데이터 재생성 없이 구조를 바꿀 수 있다.

이 이슈에서 임계값은 바꾸지 않는다. 구조 이관과 값 변경을 섞으면 회귀 원인을 분리할 수 없다.
값의 근거 확정은 상수 근거 이슈에서 다룬다.

## 체크리스트

- [ ] 조회 경로가 radiance를 읽어 등급을 계산하도록 변경하고, CSV bortle 컬럼 의존을 끊는다
  → [`LightPollutionDataProvider.java#L18-L29`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/provider/LightPollutionDataProvider.java#L18-L29)
- [ ] 변환 임계표를 Java 상수로 옮기고, 유효 등급 범위(1~9) 밖 값을 거부한다
  한국용과 전 지구용 스크립트의 무데이터 처리가 다르다(`radiance<=0`을 0등급 vs 1등급).
  0등급이 유입되면 `realBortle > 1` 조건 때문에 광해 감점이 아예 빠지는데,
  표시용 `BortleGrade.from(0)`은 Class 9를 반환해 점수와 표시가 정반대가 된다
  → [`BortleGrade.java#L26-L31`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/enums/BortleGrade.java#L26-L31)
- [ ] 좌표 축 순서를 통일한다. 적재와 박스는 `(lon, lat)`인데 거리 정렬만 `(lat, lon)`이다
  먼저 DB에서 알려진 좌표 3~5개로 `findNearest`를 실행해 어느 쪽이 틀렸는지 확인할 것.
  MySQL의 SRID 4326 축 순서 규약을 실행으로 확인하지 않았다
  → [`LightPollutionRepository.java#L20-L34`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/repository/LightPollutionRepository.java#L20-L34)
  · [`LightPollutionMigrationRunner.java#L61`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/infra/lightpollution/LightPollutionMigrationRunner.java#L61)
- [ ] 부분 적재 고착을 해소한다. `count() > 0` 판정이라 중간에 실패하면 나머지가 영구 결손된다
  → [`LightPollutionMigrationRunner.java#L31-L77`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/infra/lightpollution/LightPollutionMigrationRunner.java#L31-L77)
- [ ] 캐시 키를 정규화한다. `#lat + '-' + #lon` 원시 double이라 사실상 캐시가 동작하지 않는다
  예보 캐시는 `%.4f`로 정규화하고 있어 처리가 두 곳에서 다르다
  → [`LightPollutionDataProvider.java#L21`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/provider/LightPollutionDataProvider.java#L21)
  · [`OpenWeatherMapClient.java#L55`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/openweather/OpenWeatherMapClient.java#L55)
- [ ] 탐색 박스 ±0.01도의 근거를 격자 간격(약 0.004도)과 함께 정리하고, 폴백 발생을 계측 가능하게 한다
  → [`LightPollutionRepository.java#L15-L19`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/repository/LightPollutionRepository.java#L15-L19)

## 완료 기준

등급 변환이 Java 코드 안에서 이뤄지고, 임계표가 테스트로 고정되며,
임계값을 바꿀 때 데이터를 다시 만들지 않아도 된다.

## 결정 필요

- 등급을 조회 시점에 계산할지, 적재 시점에 계산해 둘지 (변경 유연성 vs 조회 비용)
- 무데이터 픽셀을 어떻게 표현할지 (행 제외 vs 별도 등급)

## 관련 문서

[조사 문서](https://github.com/byeolbolil/byeolbolil-backend/blob/dev/docs/issues/scoring.md)의 인벤토리 B(광해 데이터 경로), E(CSV 구조), F(저장소 밖 생성 스크립트)

