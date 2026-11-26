# 테스트 fixture

외부 API 응답을 대신하는 파일이다. `ExternalApiStubs`와 각 클라이언트 테스트가 `Fixtures.read()`로 읽는다.

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

`kakao/`의 JSON은 2026-11-27에 카카오 로컬 API에서 실제로 받은 응답이다. 키는 `Authorization: KakaoAK ...` 헤더로
보내므로 파일에 남지 않는다. 주소 앞의 `https://dapi.kakao.com`은 줄였다.

| 파일 | 요청 |
|---|---|
| `kakao/keyword-hwaaksan-20261127.json` | `GET /v2/local/search/keyword.json?query=화악산&size=15`. 산과 등산로 사이에 펜션이 섞여 온다 |
| `kakao/keyword-manghyang-20261127.json` | 같은 요청에 `query=정천면망향의광장전망대`. OpenStreetMap에 없는 전망대다 |
| `kakao/keyword-daejeon-20261127.json` | 같은 요청에 `query=대전`. 시가 아니라 시 안의 관광지가 온다 |
| `kakao/keyword-no-result-20261127.json` | 같은 요청에 `query=전북 진안군 정천면 970-40`. 지번은 키워드 검색으로 찾지 못한다 |
| `kakao/address-jibun-20261127.json` | `GET /v2/local/search/address.json?query=전북 진안군 정천면 970-40&size=5` |
| `kakao/address-daejeon-20261127.json` | 같은 요청에 `query=대전`. 시 이름은 지역 하나로 답한다 |
| `kakao/address-no-result-20261127.json` | 같은 요청에 `query=화악산`. 주소가 아닌 이름은 찾지 못한다 |
