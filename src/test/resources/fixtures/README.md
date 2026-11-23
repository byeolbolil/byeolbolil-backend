# 테스트 fixture

외부 API 응답을 대신하는 파일이다. `ExternalApiStubs`와 각 클라이언트 테스트가 `Fixtures.read()`로 읽는다.

## 출처

여기 있는 JSON은 실제 호출을 저장한 것이 아니다. 각 API가 문서로 공개한 응답 형식에 맞춰 손으로 썼고,
값은 테스트가 계산하기 쉬운 숫자로 골랐다.

| 파일 | 형식의 근거 | 내용 |
|---|---|---|
| `openweather/current-clear.json` | [Current weather data](https://openweathermap.org/current) | 맑은 밤. 구름 5%, 습도 45%, 시정 10km |
| `openweather/forecast-anbandegi-20260115.json` | [5 day / 3 hour forecast](https://openweathermap.org/forecast5) | 강릉 안반데기, 2026-01-15 18:00부터 16일 09:00까지(KST) 3시간 간격 6칸 |
| `gemini/comment-good.json` | [generateContent 응답](https://ai.google.dev/api/generate-content) | 코드 블록으로 감싼 JSON을 본문으로 주는 정상 응답 |
| `gemini/comment-not-json.json` | 같음 | 본문이 JSON이 아닌 응답 |

예보 파일의 칸별 조건은 다음과 같다.

| 시각(KST) | 구름 | 습도 | 시정 | 고른 이유 |
|---|---|---|---|---|
| 15일 18:00 | 40% | 70% | 10km | 시민박명이 막 끝난 시각. 구름과 습도 감점 |
| 15일 21:00 | 20% | 55% | 10km | 구름 감점만 |
| 16일 00:00 | 0% | 50% | 10km | 날씨 감점 없음 |
| 16일 03:00 | 80% | 85% | 8km | 구름 70% 이상이라 0점 |
| 16일 06:00 | 0% | 90% | 4km | 습도와 시정 감점. 달이 막 떠서 달 감점도 붙는다 |
| 16일 09:00 | 0% | 60% | 10km | 낮이라 예보 응답에서 빠진다 |

## 실제 응답으로 바꾸려면

API 키를 넣어 한 번 호출한 결과를 같은 경로에 저장하면 된다. 키가 응답에 들어가지는 않지만 요청 주소에는 들어가므로
명령을 그대로 커밋하지 않는다. 값이 달라지면 이 파일을 쓰는 테스트의 기대값도 같이 고쳐야 한다.

```bash
curl "https://api.openweathermap.org/data/2.5/forecast?lat=37.6254&lon=128.9392&units=metric&appid=$WEATHER_API_KEY" \
  > src/test/resources/fixtures/openweather/forecast-anbandegi-YYYYMMDD.json
```
