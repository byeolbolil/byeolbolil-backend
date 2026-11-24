# 테스트 fixture

외부 API 응답을 대신하는 파일이다. `ExternalApiStubs`와 각 클라이언트 테스트가 `Fixtures.read()`로 읽는다.

## 손으로 쓴 파일

`openweather/`와 `gemini/`의 JSON은 실제 호출을 저장한 것이 아니다. 각 API가 문서로 공개한 응답 형식에 맞춰 손으로 썼고,
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

## 실제 응답을 저장한 파일

`openmeteo/`와 `metnorway/`의 JSON은 2026-11-23 18:49(UTC)에 실제로 받은 응답을 고치지 않고 저장한 것이다.
좌표는 강릉 안반데기가 속한 예보 칸의 중심(37.65, 128.95)이다. 이 파일을 쓰는 테스트는 현재 시각을 같은 날 18:50으로 고정한다.

| 파일 | 요청 |
|---|---|
| `openmeteo/forecast-ecmwf-ifs-anbandegi-20261123.json` | `GET https://api.open-meteo.com/v1/forecast?latitude=37.65&longitude=128.95&hourly=cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high,relative_humidity_2m,dew_point_2m,visibility,temperature_2m&forecast_days=9&timeformat=unixtime&timezone=GMT&models=ecmwf_ifs` |
| `openmeteo/meta-ecmwf-ifs-20261123.json` | `GET https://api.open-meteo.com/data/ecmwf_ifs/static/meta.json` |
| `openmeteo/cloud-grid-2x3-ecmwf-ifs-20261124.json` | `GET https://api.open-meteo.com/v1/forecast?latitude=37.0,37.0,37.0,37.4,37.4,37.4&longitude=127.0,127.4,127.8,127.0,127.4,127.8&hourly=cloud_cover&forecast_days=9&timeformat=unixtime&timezone=GMT&models=ecmwf_ifs`. 2026-11-24(UTC)에 받았다. 구름 격자 요청의 형식을 2줄 3칸으로 줄여 받은 것이다 |
| `metnorway/complete-anbandegi-20261123.json` | `GET https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=37.65&lon=128.95` |

MET Norway 응답의 헤더는 파일에 없다. 받을 때의 값은 `Expires: Mon, 23 Nov 2026 19:18:57 GMT`였고, 테스트가 이 값을 직접 넣는다.
두 API 모두 키가 필요 없다. MET Norway는 약관상 서비스를 식별하는 User-Agent 헤더를 요구한다.

`photon/`의 JSON은 2026-11-25에 실제로 받은 응답을 고치지 않고 저장한 것이다. Photon은 키도 User-Agent 조건도 없다.
주소 앞의 `https://photon.komoot.io`는 줄였다.

| 파일 | 요청 |
|---|---|
| `photon/search-hwaaksan-20261125.json` | `GET /api/?q=화악산&limit=30&lang=default&bbox=124.5,33.0,131.9,38.7` |
| `photon/search-yangpyeong-20261125.json` | 같은 요청에 `q=양평`. 이름이 같은 역과 정류장이 양평군보다 앞에 온다 |
| `photon/search-no-result-20261125.json` | 같은 요청에 `q=육백마지기`. 결과가 없다 |
| `photon/reverse-anbandegi-20261125.json` | `GET /reverse?lat=37.623&lon=128.739&limit=1&lang=default&radius=10` |
| `photon/reverse-seoul-20261125.json` | 같은 요청에 `lat=37.57&lon=126.89`. 응답에 도가 없다 |
| `photon/reverse-sea-20261125.json` | 같은 요청에 `lat=36.5&lon=130.5`. 동해 한가운데라 결과가 없다 |

## 손으로 쓴 파일을 실제 응답으로 바꾸려면

API 키를 넣어 한 번 호출한 결과를 같은 경로에 저장하면 된다. 키가 응답에 들어가지는 않지만 요청 주소에는 들어가므로
명령을 그대로 커밋하지 않는다. 값이 달라지면 이 파일을 쓰는 테스트의 기대값도 같이 고쳐야 한다.

```bash
curl "https://api.openweathermap.org/data/2.5/forecast?lat=37.6254&lon=128.9392&units=metric&appid=$WEATHER_API_KEY" \
  > src/test/resources/fixtures/openweather/forecast-anbandegi-YYYYMMDD.json
```
