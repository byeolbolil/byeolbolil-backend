점수 계산에 관여하는 상수 40여 개 중 출처 주석이 달린 것이 하나도 없다.
CLAUDE.md의 "도메인 값에는 추적 가능한 출처" 규약을 전부 위반하고 있다.

이 이슈의 본체는 코딩이 아니라 근거 조사와 문서화다.
출처를 찾지 못한 값은 추측으로 채우지 말고 "출처 불명"으로 명시한다.

## 체크리스트

- [ ] 광해 `(Bortle-1) × 6.25`. 최대 -50점을 좌우한다
  → [`StargazingScoringEngine.java#L96-L103`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L96-L103)
- [ ] 달 `밝기 × 30`과 감점 1점 미만 절삭 규칙
  → [`StargazingScoringEngine.java#L85-L94`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L85-L94)
- [ ] 구름 컷오프 70, 무시 구간 10, 기울기 1. 69%는 41점인데 70%는 0점이라 1%p 차이에 41점이 갈린다
  → [`StargazingScoringEngine.java#L46-L63`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L46-L63)
- [ ] 습도 `(습도-60)/2`. DESIGN_DOC은 "습도 30% 미만"이라 코드와 다르다
  → [`StargazingScoringEngine.java#L66-L72`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L66-L72)
- [ ] 시정 임계 5000/10000과 감점 -20/-10, 그리고 라벨용 4단계 표와의 이원화 정리
  → [`VisibilityGrade.java#L8-L26`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/enums/VisibilityGrade.java#L8-L26)
- [ ] radiance→Bortle 임계표 0.25 / 0.50 / 1.00 / 2.50 / 5.00 / 10.0 / 25.0 / 50.0
  저장소 밖 `extract_korea_bortle.py`에만 있고, 근거는 "근사치"라는 주석 한 줄이 전부다.
  원본 래스터(VNL v2 2024)의 획득 경로와 라이선스도 기록이 없다. 스크립트와 획득 절차를 저장소로 들여올 것
- [ ] 감점 총합 상한 정책을 정한다
  항목별 최대를 더하면 179점이라 100을 넘길 수 있고, 0으로 클램프되면서 "구름 컷오프", "낮", "누적 초과"가 구분되지 않는다
  → [`StargazingScoringEngine.java#L105-L106`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/src/main/java/xyz/ncookie/stargazer/domain/stargazing/engine/StargazingScoringEngine.java#L105-L106)
- [ ] README와 DESIGN_DOC을 코드에 맞춘다. 확인된 불일치 5건:
  DESIGN_DOC 습도 30% vs 코드 60% / README 구름 "최대 감점: 관측 불가"(실제 -59점) /
  README "-6° 이상 0점" vs 코드 `> -6.0` / 달 절삭 규칙 미기재 / 광해 폴백 4등급 미기재
  → [`README.md#L39-L56`](https://github.com/byeolbolil/byeolbolil-backend/blob/8ae8b3a/README.md#L39-L56)

## 완료 기준

- `docs/SCORING.md`가 있고, 코드의 모든 점수 상수가 그 문서의 항목과 1:1로 대응한다
- 출처를 찾지 못한 값은 "출처 불명"으로 명시돼 있다
- README와 DESIGN_DOC이 코드와 일치한다

## 결정 필요

이 이슈는 전체가 결정 작업이다. 각 계수의 근거를 무엇으로 채택할지,
절대 감점에 클램프를 씌운 현재 구조를 유지할지 가중합이나 정규화로 바꿀지, 총합 상한을 둘지.

## 관련 문서

상수 40여 개의 위치와 용도, README 대조는 [조사 문서](https://github.com/byeolbolil/byeolbolil-backend/blob/dev/docs/issues/scoring.md)의 1단계 인벤토리에 정리돼 있다.

