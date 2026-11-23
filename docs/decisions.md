# 결정 기록

정책, 구조, 성능, 배포에서 실제로 선택을 한 지점을 날짜순으로 남긴다.
각 항목은 무엇을 골랐는지, 왜 골랐는지, 무엇을 버렸는지, 어디에 영향을 주는지를 적는다.
점수 상수의 근거와 출처는 여기가 아니라 `docs/SCORING.md`와 사용 지점 코드 주석에 둔다.

## 2026-09-04 Gemini 모델명을 설정값으로 분리 (미적용)

### 배경

`GeminiAnalysisClient`가 호출 URL에 모델명을 문자열로 박아 쓰고 있다.
2026-09-04 Google이 `gemini-2.0-flash`의 `generateContent` 지원을 종료하면서
운영에서 404가 발생했다.

```
2026-09-04 16:22:56 ERROR x.n.s.d.s.c.gemini.GeminiAnalysisClient : Gemini Error
404 Not Found on POST .../v1beta/models/gemini-2.0-flash:generateContent
"This model models/gemini-2.0-flash is no longer available.
 Please update your code to use models/gemini-3.6-flash ..."
```

모델명이 들어가는 위치는 한 곳뿐이다.
[`GeminiAnalysisClient.java#L83`](../src/main/java/xyz/ncookie/stargazer/domain/stargazing/client/gemini/GeminiAnalysisClient.java#L83).
`application.yml`의 `gemini` 블록에는 `api.key`만 외부화돼 있고 모델명은 없다.

당장의 404는 하드코딩된 문자열을 `gemini-3.7-flash`로 올려 막아둔 상태다.
아래는 그 임시 조치가 아니라 반복을 막기 위한 후속 작업을 정한 것이다.

### 선택

모델명을 `gemini.api.model` 설정값으로 분리하고 환경변수 `GEMINI_MODEL`로 주입한다.
기본값은 코드에 두되, 모델이 폐기될 때 코드 수정과 재배포 없이 환경변수만 바꿔
대응할 수 있게 한다.

- `application.yml`에 `gemini.api.model: ${GEMINI_MODEL:<기본모델>}`을 추가한다.
- `GeminiAnalysisClient`가 `@Value("${gemini.api.model}")`로 주입받아 URL을 조립한다.
- `.github/workflows/deploy.yml`의 `docker run`에 `-e GEMINI_MODEL`을 넘긴다. 기본값이
  있으므로 필수는 아니고, 운영에서 즉시 바꿔야 할 때를 위한 통로다.

기본 모델 문자열은 적용 시점에 사용 가능 목록을 확인해 정한다.
확인은 `GET https://generativelanguage.googleapis.com/v1beta/models?key=$GEMINI_API_KEY`로 한다.
Google 안내 문구는 `gemini-3.6-flash`를 가리켰고, 현재 코드에는 `gemini-3.7-flash`가 들어가 있다.

### 버린 대안

폐기될 때마다 URL의 모델 문자열을 직접 고쳐 커밋하는 방식. 지금까지 해온 방식이고
변경량은 작지만, 모델 폐기가 반복되는 외부 API에서는 그때마다 코드 변경과 배포가
필요해 장애 대응이 느리다.

### 적용 상태

미적용. 이 문서에 남기고 별도 작업으로 처리한다.

### 영향

- AI 코멘트 기능: 모델이 유효한 동안에는 정상 동작한다. 호출이 실패하면
  `GeminiAnalysisClient`의 `catch`가 폴백값(`"AI 연결 지연"`, `"Class ?"`,
  `"알 수 없음"`, `"?등급"`)을 반환하고, 점수 자체는 기상 점수를 그대로 쓰므로
  응답은 유지된다.
- 클라이언트: 응답 스키마는 바뀌지 않는다. `comment`, `bortle`, `brightness`,
  `limiting_mag`가 폴백값으로 내려갈 수 있다는 점을 `docs/MOBILE_DEVELOPER_GUIDE.md`에
  반영할지 검토한다.
