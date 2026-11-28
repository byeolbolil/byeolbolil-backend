# 별볼일 API

별볼일 백엔드가 제공하는 HTTP API를 클라이언트 쪽에서 보는 문서다. 웹과 모바일 앱을 만드는 사람, 그리고 그 일을 돕는
코드 생성 도구와 AI 에이전트가 함께 읽는다고 보고 썼다.

여기 적힌 요청과 응답은 2026-11-24에 통합 테스트와 운영 서버 응답으로 확인한 것이다. 타입과 필드 목록은 OpenAPI 스펙이
가장 정확하고, 이 문서는 스펙에 드러나지 않는 것을 맡는다. 공통 응답 형식, 인증 흐름, 오류가 어떤 모양으로 오는지,
값이 빠지는 조건 같은 것들이다. 도구에 맡길 때는 이 문서와 OpenAPI JSON을 함께 넣는다.

| | |
|---|---|
| 운영 주소 | `https://api.byeolbolil.com` |
| 로컬 주소 | `http://localhost:8080` |
| OpenAPI JSON | [`https://api.byeolbolil.com/v3/api-docs`](https://api.byeolbolil.com/v3/api-docs) |
| Swagger UI | [`https://api.byeolbolil.com/swagger-ui/index.html`](https://api.byeolbolil.com/swagger-ui/index.html) |

운영 서버는 `dev` 브랜치에 push될 때마다 다시 배포되므로 위 OpenAPI JSON은 늘 지금 돌고 있는 코드의 스펙이다.
배포하는 동안 1분쯤 응답이 끊긴다.

## 목차

1. [기본 규칙](#1-기본-규칙)
2. [공통 응답 형식](#2-공통-응답-형식)
3. [오류](#3-오류)
4. [인증](#4-인증)
5. [엔드포인트 요약](#5-엔드포인트-요약)
6. [밤 추천](#6-밤-추천)
7. [관측지](#7-관측지)
8. [지명 검색](#8-지명-검색)
9. [구름 지도](#9-구름-지도)
10. [북마크](#10-북마크)
11. [회원](#11-회원)
12. [인증 엔드포인트](#12-인증-엔드포인트)
13. [알려진 한계](#13-알려진-한계)
14. [문서를 고칠 때](#14-문서를-고칠-때)

## 1. 기본 규칙

- 모든 경로는 `/api/v1`로 시작한다.
- 요청과 응답 본문은 UTF-8 JSON이다. 본문이 있는 요청에는 `Content-Type: application/json`을 붙인다. 빠지면 415다.
- 조회 조건은 쿼리 문자열로 보낸다. `GET` 요청에 본문을 쓰는 API는 없다.
- 날짜는 `yyyy-MM-dd`, 시각은 `HH:mm`이다. 응답의 시점 값은 한국 시간 오프셋이 붙은 ISO 8601 문자열이다
  (`2026-11-24T22:00:00+09:00`). 서버는 한국 시간만 쓴다.
- 좌표의 쿼리 이름은 `lat`, `lon`이다. 응답과 북마크 본문에서는 `latitude`, `longitude`다.
- 로그인이 필요한 API에는 `Authorization: Bearer {accessToken}` 헤더를 붙인다.
- 호출 횟수 제한은 없다. 다만 서버가 메모리 1GB짜리 한 대라서, 화면 하나에 필요한 만큼만 부른다.
- 웹 브라우저에서 직접 부를 수 있는 출처는 `https://www.byeolbolil.com`, `https://api.byeolbolil.com`,
  `http://localhost:3000`, `http://localhost:8080`이다. 옛 도메인 `byeolbolil.xyz`의 두 주소도 아직 받는다. 네이티브 앱은 CORS와 상관없다.

## 2. 공통 응답 형식

Swagger 경로를 뺀 모든 응답은 같은 봉투에 담겨 온다.

```json
{
  "success": true,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { "memberId": 1, "nickname": "별지기" }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `success` | boolean | 성공이면 `true` |
| `status` | number | HTTP 상태 코드와 같은 값 |
| `message` | string | 사람이 읽는 문장. 성공일 때는 늘 위 문구다 |
| `data` | any | 엔드포인트마다 다른 본문. 없으면 키 자체가 빠진다 |

클라이언트는 `data`만 꺼내 쓰면 된다. 목록을 주는 API는 `data`가 배열이다.

돌려줄 것이 없는 성공(로그아웃, 북마크 삭제)은 `data` 없이 온다.

```json
{ "success": true, "status": 200, "message": "요청이 성공적으로 처리되었습니다." }
```

성공은 항상 200이다. 201이나 204는 쓰지 않는다.

OpenAPI 스펙에는 이 봉투가 나오지 않는다. 스펙의 응답 스키마는 `data` 안쪽만 그린다. 스펙으로 클라이언트 코드를
생성했다면 응답에서 봉투를 벗겨 `data`를 넘겨주는 단계를 한 곳에 둔다. 오류 응답의 모양도 스펙에는 없으므로
이 문서의 [3장](#3-오류)을 따른다.

`data` 안에서 값이 없는 필드는 키가 빠지기도 하고 `null`로 오기도 한다. 어느 쪽인지는 필드 표에 적어 두었고,
클라이언트는 두 경우를 같은 것으로 다루면 된다.

## 3. 오류

오류도 같은 봉투에 담기고 `success`가 `false`다. 모양은 세 가지다.

도메인 오류는 `message`만 있다.

```json
{ "success": false, "status": 409, "message": "이미 사용 중인 이메일입니다." }
```

입력 검증 실패는 400이고 `data`에 이름별 사유가 담긴다.

```json
{
  "success": false,
  "status": 400,
  "message": "유효성 검사에 실패하였습니다.",
  "data": {
    "radius": "반경은 10km 이상이어야 합니다."
  }
}
```

`data`의 키는 보통 요청 필드나 쿼리 이름이다. 경로 변수의 형식이 틀리면 그 변수 이름이 키가 된다
(`bookmarkId`, `date`). 여러 값을 함께 보는 검증은 필드 이름이 아닌 키로 온다.

| 키 | 뜻 |
|---|---|
| `originComplete` | 밤 추천에서 `lat`과 `lon` 가운데 하나만 보냈다 |
| `spotValid` | `SPOT` 북마크에 `spotId`가 없다 |
| `customValid` | `CUSTOM` 북마크에 좌표가 없다 |

형식을 바꾸지 못한 값의 사유는 "입력 형식이 올바르지 않습니다."다.

경로나 메서드가 틀린 경우처럼 프레임워크가 걸러 낸 오류는 `message`가 영어일 수 있다.

```json
{ "success": false, "status": 405, "message": "Method 'POST' is not supported." }
```

### 상태 코드

| 상태 | 뜻 | 클라이언트가 할 일 |
|---|---|---|
| 400 | 입력이 틀렸다 | `data`가 있으면 필드별로, 없으면 `message`를 보여 준다 |
| 401 | 토큰이 없거나 통하지 않는다. 로그인에서는 이메일이나 비밀번호가 틀렸다 | [4.3](#43-401을-받았을-때)을 따른다 |
| 403 | 로그인은 했지만 내 것이 아니다 | 재발급하지 않는다 |
| 404 | 경로나 대상이 없다 | |
| 405, 415 | 메서드나 `Content-Type`이 틀렸다 | 클라이언트 코드의 버그다 |
| 409 | 이미 있는 것과 겹친다 | 이미 가입된 이메일, 이미 저장한 명소 |
| 429 | 지명 검색 요청이 몰렸다 | 1초쯤 뒤에 다시 보낸다 |
| 500 | 서버 오류 | `message`에 내부 예외 문구가 실릴 수 있으니 그대로 보여 주지 않는다 |
| 503 | 날씨 예보나 지명 데이터를 받지 못했다 | 잠시 뒤 다시 시도한다 |

### 오류를 구분하는 법

오류 응답에는 기계가 읽는 코드가 없다. 상태 코드와 어느 요청이었는지로 구분하고, `message`는 화면에 보여 주는
용도로만 쓴다. `message` 문자열에 조건을 걸지 않는다. 문구를 따로 정하지 않은 검증 사유는 요청의
`Accept-Language`에 따라 언어가 바뀐다. 같은 요청에 `ko-KR`이면 "공백일 수 없습니다", `en-US`면
"must not be blank"가 온다. 한국어 문구를 받으려면 `Accept-Language: ko-KR`을 보낸다.

### 도메인 오류 목록

| 상태 | `message` | 나오는 곳 |
|---|---|---|
| 400 | 오늘 밤부터 7일 안의 날짜만 볼 수 있습니다. | 날짜별 밤 추천 |
| 400 | 볼 위치의 위도와 경도를 보내야 합니다. | 한 위치의 이번 주 밤 |
| 400 | 북마크 타입과 데이터가 일치하지 않습니다. | 북마크 추가. `CUSTOM`에 `address`가 없을 때 |
| 400 | 요청 본문을 읽을 수 없습니다. | 본문이 JSON이 아니거나 enum에 없는 값을 보냈을 때 |
| 400 | 인증 코드가 맞지 않습니다. | 회원가입 |
| 400 | 인증 코드를 다시 받아 주세요. | 회원가입. 코드가 없거나 지났거나 다섯 번 틀렸을 때 |
| 400 | 닉네임은 2~16자의 한글, 영문, 숫자, 밑줄로 쓰고 낱말 사이에는 공백을 하나만 둘 수 있습니다. | 회원가입, 닉네임 수정, 닉네임 중복 검사 |
| 401 | 인증이 필요합니다. | 로그인이 필요한 모든 API |
| 401 | 이메일 또는 비밀번호가 일치하지 않습니다. | 로그인 |
| 401 | 유효하지 않은 Refresh Token 입니다. | 재발급 |
| 403 | 해당 북마크를 수정 또는 삭제할 권한이 없습니다. | 북마크 수정, 삭제 |
| 403 | 관리자만 할 수 있습니다. | 닉네임 초기화 |
| 404 | 요청한 경로를 찾을 수 없습니다. | 없는 경로 |
| 404 | 해당 ID의 북마크 데이터를 찾을 수 없습니다. | 북마크 수정, 삭제 |
| 404 | 해당 ID의 명소 데이터를 찾을 수 없습니다. | 북마크 추가 |
| 404 | 사용자를 찾을 수 없습니다 | 토큰의 회원이 지워졌을 때 |
| 409 | 이미 사용 중인 이메일입니다. | 회원가입 |
| 409 | 이미 쓰는 닉네임입니다. | 회원가입, 닉네임 수정 |
| 409 | 이미 해당 명소는 북마크 되어있습니다. | 북마크 추가 |
| 429 | 지명 검색 요청이 많습니다. 잠시 후 다시 시도해 주세요. | 지명 검색 |
| 429 | 인증 코드는 1분에 한 번 받을 수 있습니다. | 가입 인증 코드 받기 |
| 429 | 오늘은 인증 코드를 더 받을 수 없습니다. 내일 다시 시도해 주세요. | 가입 인증 코드 받기 |
| 503 | 날씨 예보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요. | 예보를 쓰는 API |
| 503 | 인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요. | 가입 인증 코드 받기 |
| 503 | 지명 검색을 지금 쓸 수 없습니다. 잠시 후 다시 시도해 주세요. | 지명 검색 |

## 4. 인증

### 4.1 토큰

| | 액세스 토큰 | 리프레시 토큰 |
|---|---|---|
| 수명 | 15분 | 14일 |
| 받는 곳 | 응답 본문의 `data.accessToken` | 응답의 `Set-Cookie` 헤더, 쿠키 이름 `refresh_token` |
| 보내는 곳 | `Authorization: Bearer {토큰}` | 쿠키. 또는 `Authorization-Refresh: Bearer {토큰}` 헤더 |
| 쓰는 곳 | 로그인이 필요한 모든 API | `POST /api/v1/auth/reissue`, `POST /api/v1/auth/logout` |

둘 다 JWT지만 서로 바꿔 쓸 수 없다. 리프레시 토큰을 `Authorization` 헤더에 넣으면 401이다.

리프레시 토큰 쿠키는 이렇게 내려간다.

```
Set-Cookie: refresh_token=eyJ...; Path=/; Max-Age=1209600; Secure; HttpOnly; SameSite=None
```

- 웹은 요청에 쿠키를 싣도록 설정한다(`fetch`의 `credentials: "include"`, axios의 `withCredentials: true`).
  `HttpOnly`라 스크립트에서 값을 읽을 수 없고, 읽을 필요도 없다.
- 네이티브 앱은 둘 가운데 하나를 고른다. HTTP 클라이언트의 쿠키 저장소를 켜 두면 웹과 똑같이 동작한다.
  쿠키 저장소를 쓰지 않으려면 `Set-Cookie` 헤더에서 `refresh_token` 값을 꺼내 안전한 저장소에 두고,
  재발급과 로그아웃 때 `Authorization-Refresh: Bearer {값}` 헤더로 보낸다. 쿠키와 헤더가 함께 오면 쿠키를 쓴다.
- `Secure` 쿠키라 로컬 서버를 `http`로 띄우면 브라우저에 따라 저장되지 않을 수 있다.

### 4.2 흐름

1. `POST /api/v1/auth/register` 또는 `POST /api/v1/auth/login`으로 두 토큰을 받는다.
2. 액세스 토큰을 `Authorization` 헤더에 넣어 API를 부른다.
3. 액세스 토큰이 끝나면 `POST /api/v1/auth/reissue`로 두 토큰을 새로 받는다.
4. `POST /api/v1/auth/logout`으로 리프레시 토큰을 없앤다. 액세스 토큰은 클라이언트가 지운다.

리프레시 토큰은 한 번 쓰면 폐기되고 새것이 온다. 폐기된 토큰으로 다시 재발급하면 401이다. 그래서 재발급은 한 번에
하나만 보내야 한다. 여러 요청이 동시에 401을 받았다면 첫 요청만 재발급을 보내고 나머지는 그 결과를 기다렸다가
새 액세스 토큰으로 다시 보낸다.

로그아웃해도 이미 받은 액세스 토큰은 남은 수명(길어야 15분) 동안 통한다.

### 4.3 401을 받았을 때

- 로그인이 필요한 API가 401이면 재발급을 한 번 하고 원래 요청을 다시 보낸다.
- 재발급이 401이면 로그인이 풀린 것이다. 저장한 토큰을 지우고 로그인 화면으로 보낸다.
- `POST /api/v1/auth/login`의 401은 이메일이나 비밀번호가 틀렸다는 뜻이다. 재발급하지 않는다.
- 토큰이 없을 때, 깨졌을 때, 만료됐을 때의 401은 서로 구분되지 않는다. 모두 "인증이 필요합니다."다.

### 4.4 로그인이 선택인 API

밤 추천은 로그인하지 않아도 쓸 수 있고, 로그인하면 내 장소까지 함께 본다. 이런 API는 만료되거나 깨진 토큰이 붙어
와도 401을 주지 않고 로그인하지 않은 요청으로 처리한다. 응답은 200인데 내 장소만 빠진다.

로그인한 상태에서 이 API를 부를 때는 액세스 토큰의 만료 시각(JWT의 `exp`)을 먼저 보고, 지났거나 곧 지나면
재발급한 뒤에 부른다.

### 4.5 소셜 로그인

카카오, 구글, 네이버 로그인은 브라우저 리다이렉트로만 동작한다.

1. 브라우저를 `https://api.byeolbolil.com/oauth2/authorization/{kakao|google|naver}`로 보낸다.
2. 로그인에 성공하면 서버가 `refresh_token` 쿠키를 심고 `https://www.byeolbolil.com/oauth/callback`으로 돌려보낸다.
   방금 가입한 사람이면 `?welcome=1`이 붙는다.
3. 그 페이지에서 `POST /api/v1/auth/reissue`를 불러 액세스 토큰을 받는다.

소셜 가입은 제공자가 준 이름을 닉네임으로 쓰지 않는다. `별지기4821`처럼 "별지기"와 숫자 네 자리로 된, 아무도 쓰지 않는
기본 닉네임으로 가입한다. `welcome=1`을 받으면 첫 화면에서 닉네임을 정하게 한다(`PATCH /api/v1/members/me`).

로그인에 실패하면 `https://www.byeolbolil.com/login?error=oauth`로 돌려보낸다.

돌아가는 주소가 웹 프론트로 고정돼 있어 네이티브 앱에서는 쓸 수 없다. 앱에서 소셜 로그인을 하려면 서버에 앱용
흐름을 추가해야 한다. 소셜 로그인으로만 가입한 회원은 비밀번호가 없어 이메일 로그인이 되지 않는다.

## 5. 엔드포인트 요약

인증 열의 "필요"는 액세스 토큰이 있어야 한다는 뜻이고, "선택"은 [4.4](#44-로그인이-선택인-api)의 방식이다.

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/v1/recommendations/tonight` | 선택 | 오늘 밤 장소별 추천 |
| GET | `/api/v1/recommendations/nights/{date}` | 선택 | 날짜별 밤 추천 |
| GET | `/api/v1/recommendations/nights` | 선택 | 7일의 밤마다 가장 좋은 등급 |
| GET | `/api/v1/recommendations/week` | 없음 | 한 위치의 이번 주 밤 |
| GET | `/api/v1/recommendations/week/mine` | 선택 | 저장한 장소들의 이번 주 밤 |
| GET | `/api/v1/recommendations/sky` | 없음 | 한 좌표의 하늘 밝기 |
| GET | `/api/v1/recommendations/moon` | 없음 | 한 좌표의 6주 달력 |
| GET | `/api/v1/spots` | 없음 | 관측지와 천문대 목록 |
| GET | `/api/v1/geocode/search` | 없음 | 이름으로 장소 찾기 |
| GET | `/api/v1/geocode/reverse` | 없음 | 좌표의 지역 이름 |
| GET | `/api/v1/clouds/nights/{date}` | 없음 | 하룻밤의 구름 격자 |
| GET | `/api/v1/bookmarks` | 필요 | 내 북마크 목록 |
| POST | `/api/v1/bookmarks` | 필요 | 북마크 추가 |
| PATCH | `/api/v1/bookmarks/{bookmarkId}` | 필요 | 북마크 이름과 메모 수정 |
| DELETE | `/api/v1/bookmarks/{bookmarkId}` | 필요 | 북마크 삭제 |
| GET | `/api/v1/members/me` | 필요 | 내 정보 |
| PATCH | `/api/v1/members/me` | 필요 | 닉네임 수정 |
| GET | `/api/v1/members/exists/email` | 없음 | 이메일 중복 검사 |
| GET | `/api/v1/members/exists/nickname` | 선택 | 닉네임 중복 검사 |
| POST | `/api/v1/admin/members/{memberId}/nickname/reset` | 관리자 | 닉네임 초기화 |
| POST | `/api/v1/auth/email-code` | 없음 | 가입 인증 코드 받기 |
| POST | `/api/v1/auth/register` | 없음 | 회원가입 |
| POST | `/api/v1/auth/login` | 없음 | 로그인 |
| POST | `/api/v1/auth/reissue` | 리프레시 토큰 | 토큰 재발급 |
| POST | `/api/v1/auth/logout` | 리프레시 토큰 | 로그아웃 |

## 6. 밤 추천

서비스의 중심 API다. 하룻밤을 놓고 장소마다 언제 얼마나 맑은지 평가해 좋은 순서로 돌려준다.

```
GET /api/v1/recommendations/tonight
GET /api/v1/recommendations/nights/{date}
```

`{date}`는 밤이 시작하는 날짜(`yyyy-MM-dd`)다. 오늘 밤부터 7일 안의 날짜만 받는다. 벗어나면 400이다.

"오늘 밤"은 새벽 4시에 바뀐다. 새벽 4시 전에 `tonight`을 부르면 전날 저녁에 시작한 밤이 오고, 응답의 `date`도
전날 날짜다.

후보는 명소 가운데 밤에 자유롭게 갈 수 있는 관측지(`kind`가 `OPEN_SITE`인 곳) 전부다. 천문대와 과학관은
후보가 아니다. 액세스 토큰을 붙이면 내 북마크도 후보에 들어가고, 북마크로 저장한 곳은 종류를 가리지 않고 평가한다.

### 쿼리

모두 선택이다.

| 이름 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `lat`, `lon` | number | 없음 | 출발지. 둘을 함께 보내야 한다. 보내면 이동 시간을 어림해 `travel`을 채운다. 순서에는 넣지 않는다 |
| `minClearHours` | integer, 1~8 | 3 | 맑은 시간이 이만큼 이어지면 `STRONG`으로 본다 |
| `preferBefore` | `HH:mm` | `02:00` | 이 시각 전의 맑은 시간을 우선한다. 정오 전의 값은 다음 날 새벽으로 읽는다 |
| `maxTravelMinutes` | integer, 0 이상 | 없음 | 이동 시간이 이보다 긴 장소를 뺀다. 이동 시간이 어림값이라 한도의 6분의 1만큼은 넘어도 남긴다(120이면 140분까지). 출발지를 보냈을 때만 쓰인다 |

### 응답 `data`

| 필드 | 타입 | 설명 |
|---|---|---|
| `date` | string | 밤이 시작하는 날짜 |
| `criteria.minClearHours` | integer | 적용한 기준 |
| `criteria.preferBefore` | string | 적용한 기준 (`HH:mm`) |
| `criteria.maxTravelMinutes` | integer 또는 null | 보내지 않았으면 `null` |
| `candidateCount` | integer | 후보 장소 수. 그 밤에 길이 막혀 있는 명소(겨울에 통제되는 고갯길 등)는 후보에서 빠진다. 직접 저장한 장소는 통제 기간에도 후보다 |
| `evaluatedCount` | integer | 예보를 받아 평가한 수. `candidateCount`보다 작으면 일부 장소의 예보를 받지 못한 것이다 |
| `excludedByTravelCount` | integer | 이동 시간 한도로 뺀 수 |
| `places` | array | 추천 순서대로. 첫 번째가 가장 좋은 곳이다 |

`places`의 길이는 `evaluatedCount`에서 `excludedByTravelCount`를 뺀 값이다. 등급이 `NO`인 장소도 목록에 들어 있다.
예보를 전혀 받지 못하면 오류가 아니라 `evaluatedCount`가 0이고 `places`가 빈 배열인 200이 온다.

`places`의 항목은 이렇다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `type` | string | `SPOT` 기본 추천 명소, `BOOKMARK` 내 장소 |
| `id` | integer | `type`이 `SPOT`이면 명소 ID, `BOOKMARK`면 북마크 ID. 두 ID는 서로 다른 번호 체계다 |
| `spotId` | integer | 명소이거나 명소를 저장한 북마크일 때 그 명소의 ID. 직접 찍은 위치의 북마크면 빠진다 |
| `name`, `address` | string | |
| `latitude`, `longitude` | number | 명소는 `viewpoint`의 좌표다 |
| `viewpoint` | string | 실제로 차를 대고 보는 지점의 이름. 명소가 아니거나 값이 없으면 빠진다 |
| `description` | string | 어떤 곳인지 한두 문장. 명소가 아니면 빠진다 |
| `guide` | string | 가는 법과 주차. 명소가 아니면 빠진다 |
| `caution` | string | 계절 폐쇄, 조명, 통제 같은 주의 사항. 없으면 빠진다 |
| `links` | array | 참고 링크. 항목은 `title`, `url`, `imageUrl`이다. 7장 관측지의 `links`와 같다. 명소가 아니거나 링크가 없으면 빠진다 |
| `points` | array | 이 명소 안에서 서서 보는 자리들. 항목은 `name`, `access`(어떻게 가는지와 어떤 자리인지), `latitude`, `longitude`다. 첫 항목이 차를 대는 곳이고 나머지는 거기서 차로 조금 가거나 30분 안쪽으로 걸어가는 곳이다. 좌표는 아는 자리에만 있다. 자리가 하나뿐인 명소에서는 빠진다 |
| `videos` | array | 이 명소에서 별을 본 유튜브 영상. 항목은 `title`(영상 제목), `channel`(올린 채널), `url`(여는 주소), `thumbnailUrl`(320×180 미리보기 그림), `shortForm`(쇼츠인지)이다. 골라 둔 순서대로 최대 4개가 오고, 영상을 찾지 못한 명소와 시설에서는 빠진다 |
| `level` | string | 추천 등급. 아래 표 |
| `pattern` | string | 그 밤의 날씨 흐름. 아래 표 |
| `notes` | string[] | 주의할 점. 없으면 빈 배열 |
| `bestWindow` | object | 추천 시간대. 맑은 시간이 없으면 빠진다 |
| `bestWindow.start`, `.end` | string | 시작과 끝 시각 |
| `bestWindow.hours` | integer | 길이(시간) |
| `travel` | object | 출발지를 보내지 않았으면 빠진다 |
| `travel.distanceKm` | number | 직선거리 |
| `travel.minutes` | integer | 차로 걸리는 시간 어림. 직선거리의 1.3배를 시속 60km로 간다고 본 값이라 실제 길 안내와 다르다 |
| `skyBrightness` | number | 하늘 전체에서 평균한 인공 밝기가 자연 밤하늘의 몇 배인지. 0에 가까울수록 어둡다. 불빛 자료가 없는 좌표(한국 둘레 밖)에서는 빠진다 |
| `skyClass` | string | 하늘 밝기로 나눈 등급. 아래 표. `skyBrightness`가 없으면 같이 빠진다 |
| `skyRank` | integer | 하늘 밝기가 남한 땅에서 몇 번째 칸으로 어두운지(1~100). 1이면 가장 어두운 1% 안이고, 21이면 "국내 상위 21%"다. 등급이 미국 국립공원 기준이라 한국은 거의 아래 두 칸에 몰려서, 국내에서 어느 정도인지를 따로 준다. `skyBrightness`가 없으면 같이 빠진다 |
| `moonBrightness` | number | 보름달을 1로 본 그 밤의 달 밝기 |
| `moon.illuminatedFraction` | number | 달에서 밝게 보이는 면의 비율. 삭 0, 보름 1. 그 밤 자정의 값이다 |
| `moon.waxing` | boolean | 차오르는 중이면 `true`, 기우는 중이면 `false`. 북반구에서는 차오르는 달의 오른쪽이 밝다 |
| `moon.rise`, `moon.set` | string | 그날 정오부터 다음 날 정오 사이에 달이 뜨고 지는 시각. 분 단위다. 그 사이에 뜨거나 지지 않으면 빠진다 |
| `forecastSource` | string | `OPEN_METEO` 또는 `MET_NORWAY` |
| `forecastIssuedAt` | string | 예보가 만들어진 시각. 모르면 빠진다 |
| `hours` | array | 해가 진 뒤부터 뜨기 전까지의 한 시간 칸 |

`hours`의 항목은 이렇다. 한 칸은 `time`부터 한 시간이다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `time` | string | 칸이 시작하는 시각 |
| `dark` | boolean | 태양이 지평선 아래 12°보다 낮은 시간인지 |
| `clear` | boolean | 어둡고 구름이 30% 이하인지 |
| `cloudCover` | integer | 총운량(%) |
| `cloudCoverLow`, `cloudCoverMid`, `cloudCoverHigh` | integer | 높이별 운량(%) |
| `relativeHumidity` | integer | 상대습도(%) |
| `moonUp` | boolean | 달이 떠 있는지 |
| `score` | integer | 0~100. 어둡지 않은 칸은 0이다 |

예보는 지금 시각부터만 받으므로, 밤중에 `tonight`을 부르면 `hours`는 지금 시각의 칸부터 시작한다.
`MET_NORWAY`로 받은 장소는 이틀 반쯤 뒤의 밤부터 `hours`가 비어 평가에서 빠질 수 있다.

### 값의 뜻

화면에 쓰는 문구는 클라이언트가 정한다. 아래는 웹에서 쓰는 문구다.

| `level` | 문구 |
|---|---|
| `STRONG` | 별 보기 아주 좋아요 |
| `GOOD` | 별 보기 좋아요 |
| `MAYBE` | 별 보기 아쉬워요 |
| `NO` | 별 보기 어려워요 |

| `pattern` | 문구 |
|---|---|
| `CLEAR_ALL_NIGHT` | 밤새 맑아요 |
| `CLEAR_EARLY` | 초저녁이 가장 맑아요 |
| `CLEARS_LATE` | 늦은 밤에 개요 |
| `CLEARS_AGAIN` | 흐렸다가 다시 개요 |
| `BRIEF_CLEAR` | 잠깐 맑은 틈이 있어요 |
| `CLOUDY_ALL_NIGHT` | 밤새 흐려요 |

| `notes` | 뜻 |
|---|---|
| `CLEARS_LATE` | 추천 시간이 모두 선호 시각 뒤다 |
| `BRIGHT_MOON` | 달이 밝다 |
| `CITY_LIGHTS` | 도시 불빛이 강하다. 은하수가 보이지 않을 만큼 하늘이 밝거나 서 있는 곳 둘레가 밝아서 등급을 낮췄다 |
| `LIGHTS_IN_VIEW` | 아래쪽 마을이나 도시의 불빛이 산에 가리지 않고 직접 보인다. 등급은 그대로다. 미리 계산해 둔 명소에서만 온다 |
| `DEW` | 이슬이 맺힐 만큼 습하다 |

| `skyClass` | `skyBrightness` | 뜻 |
|---|---|---|
| `NATURAL` | 0.33 미만 | 은하수가 무늬까지 또렷하다 |
| `DARK` | 0.33 이상 2 미만 | 은하수가 잘 보인다. 지평선 여러 방향에 불빛 돔이 보인다 |
| `RURAL` | 2 이상 10 미만 | 은하수가 머리 위에서는 보이지만 흐리고, 지평선 쪽은 불빛에 묻힌다 |
| `SUBURBAN` | 10 이상 50 미만 | 은하수가 보이지 않는다. 밝은 별과 별자리는 보인다 |
| `URBAN` | 50 이상 | 눈이 어둠에 적응하지 못할 만큼 밝다. 밝은 별만 보인다 |

뜻은 각 구간에 묶인 Bortle 등급의 설명이다([SCORING.md](./SCORING.md) "광해"). enum 이름은 처음 붙인 그대로이고 장소의
종류를 뜻하지 않는다. 화면에는 보이는 것으로 이름을 붙인다.

한국의 추천 후보지는 1.5(영양)에서 9.5 사이이고 서울 한복판이 68쯤이다. 한국 본토는 거의 `DARK`와 `RURAL`에 들어서,
국내에서 어느 정도인지는 `skyRank`로 본다.

값 목록은 늘어날 수 있다. 모르는 값이 와도 앱이 죽지 않게 기본 처리를 둔다.

등급과 점수를 어떻게 매기는지는 [SCORING.md](./SCORING.md)에 있다.

### 예시

```
GET /api/v1/recommendations/tonight?lat=37.5563&lon=126.9236&maxTravelMinutes=180
```

```json
{
  "success": true,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "date": "2026-11-25",
    "criteria": { "minClearHours": 3, "preferBefore": "02:00", "maxTravelMinutes": 180 },
    "candidateCount": 24,
    "evaluatedCount": 24,
    "excludedByTravelCount": 18,
    "places": [
      {
        "type": "SPOT",
        "id": 13,
        "spotId": 13,
        "name": "제천 덕주산성",
        "address": "충청북도 제천시 한수면 미륵송계로 1405",
        "latitude": 36.86206,
        "longitude": 128.09643,
        "viewpoint": "덕주산성 앞 주차장",
        "description": "월악산 자락 도로변에 성문이 남아 있는 산성이에요. 성문 위로 뜨는 은하수 사진으로 알려져 있고, 사방이 산이라 주변 불빛이 거의 들어오지 않아요.",
        "guide": "성문 앞 도로변 주차장에 대요.",
        "caution": "월악산국립공원 구역이에요. 밤에는 탐방로로 들어가지 말고 성문 주변에서 보세요. 은하수는 5월이 지나야 성 위로 올라와요.",
        "links": [
          {
            "title": "뚜벅이들을 위한 은하수지도: 덕주산성",
            "url": "https://gall.dcinside.com/board/view/?id=dcbest&no=6620"
          }
        ],
        "level": "GOOD",
        "pattern": "CLEAR_ALL_NIGHT",
        "notes": ["BRIGHT_MOON", "DEW"],
        "bestWindow": {
          "start": "2026-11-25T19:00:00+09:00",
          "end": "2026-11-26T06:00:00+09:00",
          "hours": 11
        },
        "travel": { "distanceKm": 129.4, "minutes": 168 },
        "skyBrightness": 3.99,
        "skyClass": "RURAL",
        "skyRank": 34,
        "moonBrightness": 0.74,
        "moon": {
          "illuminatedFraction": 0.98,
          "waxing": false,
          "rise": "2026-11-25T17:26:00+09:00",
          "set": "2026-11-26T09:08:00+09:00"
        },
        "forecastSource": "OPEN_METEO",
        "forecastIssuedAt": "2026-11-25T09:00:00+09:00",
        "hours": [
          {
            "time": "2026-11-25T19:00:00+09:00",
            "dark": true,
            "clear": true,
            "cloudCover": 0,
            "cloudCoverLow": 0,
            "cloudCoverMid": 0,
            "cloudCoverHigh": 0,
            "relativeHumidity": 78,
            "moonUp": true,
            "score": 68
          }
        ]
      }
    ]
  }
}
```

`places`와 `hours`는 하나씩만 남기고 줄였다. 실제로는 이 요청에 장소 6곳, 장소마다 시간 칸 12개가 왔다.

### 응답 시간

예보를 처음 받는 장소가 많으면 몇 초 걸린다. 운영 서버에서 캐시가 비었을 때 7.7초, 캐시가 있을 때 0.6초였다.
기상 모델이 6시간마다 새로 돌고, 그때마다 예보를 다시 받는다. 요청 제한 시간은 15초 넘게 잡고, 기다리는 동안
보여 줄 화면을 둔다.

### 예보 출처 표기

두 예보 자료 모두 CC BY 4.0이라 추천을 보여 주는 화면에 출처를 적어야 한다. `forecastSource`가 `OPEN_METEO`면
Open-Meteo(`https://open-meteo.com/`), `MET_NORWAY`면 MET Norway다. 웹은 출처 이름에 링크를 걸고 옆에
"CC BY 4.0"을 적는다. 장소마다 출처가 다를 수 있으므로 장소별 값을 본다.

### 밤마다 가장 좋은 등급

```
GET /api/v1/recommendations/nights
```

오늘 밤부터 7일의 밤마다, 후보 가운데 가장 좋은 등급과 별 보기 좋은 곳의 수를 돌려준다. 밤을 고르는 버튼에 그 밤이
얼마나 좋은지, 갈 만한 곳이 몇 곳인지 미리 보여 주려고 만들었다. 밤을 하나씩 불러 보지 않아도 어느 밤이 좋은지 알 수 있다.

쿼리와 후보는 밤 추천과 같다. 출발지와 `maxTravelMinutes`를 보내면 한도 안의 장소만 보고, 액세스 토큰을 붙이면 내
북마크도 본다. 그래서 같은 쿼리로 `/nights/{date}`를 부르면 `places`의 첫 장소가 여기서 받은 `level`과 같은 등급이다.
`NightSummaryApiTest`가 이 일치를 확인한다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `startDate` | string | 오늘 밤의 날짜. 새벽 4시 전에는 전날이다 |
| `criteria` | object | 밤 추천의 `criteria`와 같다 |
| `nights` | array | `startDate`부터 날짜순으로 항상 7개 |
| `nights[].date` | string | 밤이 시작하는 날짜 |
| `nights[].level` | string | 그 밤의 후보 가운데 가장 좋은 등급. 평가한 장소가 없으면 빠진다 |
| `nights[].goodCount` | integer | 그 밤에 `STRONG`이나 `GOOD`인 후보의 수. 같은 쿼리의 `/nights/{date}`에서 그 등급인 장소의 수와 같다. `level`이 빠지면 같이 빠진다 |

`level`이 빠지는 경우는 셋이다. 예보가 그 밤까지 닿지 않을 때, 예보를 받지 못했을 때, 이동 시간 한도 안에 후보가
하나도 없을 때다. 어느 경우에도 오류가 아니라 200이 온다. 화면에서는 그 밤의 표시를 비워 둔다.

```json
{
  "success": true,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "startDate": "2026-11-09",
    "criteria": { "minClearHours": 3, "preferBefore": "02:00", "maxTravelMinutes": null },
    "nights": [
      { "date": "2026-11-09", "level": "STRONG", "goodCount": 1 },
      { "date": "2026-11-10", "level": "NO", "goodCount": 0 },
      { "date": "2026-11-11", "level": "STRONG", "goodCount": 1 },
      { "date": "2026-11-12", "level": "NO", "goodCount": 0 },
      { "date": "2026-11-13", "level": "NO", "goodCount": 0 },
      { "date": "2026-11-14", "level": "STRONG", "goodCount": 1 },
      { "date": "2026-11-15", "level": "STRONG", "goodCount": 1 }
    ]
  }
}
```

`NightSummaryApiTest`가 고정한 예보로 받은 응답이다.

후보 전부의 일주일을 평가하므로 밤 추천 한 번보다 계산이 많다. 예보는 밤 추천과 같은 캐시를 쓴다. 밤 추천과
동시에 불러도 되지만, 화면은 밤 추천을 먼저 그리고 이 응답은 도착하는 대로 덧붙이는 편이 낫다.

### 한 장소의 이번 주

```
GET /api/v1/recommendations/week?lat={위도}&lon={경도}
GET /api/v1/recommendations/week/mine
```

밤 추천이 "그 밤에 어디가 좋은가"라면 이쪽은 "그 장소는 이번 주 언제가 좋은가"다. 한 장소의 예보 하나로 오늘
밤부터 7일을 밤마다 평가한다. 평가 방식과 값의 뜻은 밤 추천과 같고, 이동 시간은 보지 않는다.

- `/week`는 좌표로 고른 한 위치를 본다. 명소가 아니어도 되고 인증이 필요 없다. `lat`, `lon`이 필수다.
- `/week/mine`은 저장한 장소 전부를 한 번에 본다. 로그인이 선택인 API라서([4.4](#44-로그인이-선택인-api))
  로그인하지 않았으면 `places`가 빈 배열로 온다.

| 쿼리 | 타입 | 설명 |
|---|---|---|
| `lat`, `lon` | number | `/week`에서 볼 위치. `/week/mine`에서는 쓰지 않는다 |
| `minClearHours` | integer, 1~8 | 밤 추천과 같다. 기본 3 |
| `preferBefore` | `HH:mm` | 밤 추천과 같다. 기본 `02:00` |

| 필드 | 타입 | 설명 |
|---|---|---|
| `startDate` | string | 오늘 밤의 날짜. 새벽 4시 전에는 전날이다 |
| `dates` | string[] | `startDate`부터 7일. 표의 열을 그릴 때 쓴다 |
| `criteria` | object | `minClearHours`, `preferBefore` |
| `places` | array | `/week`는 그 위치 하나, `/week/mine`은 저장한 장소들 |

`places`의 항목은 다음과 같다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `type`, `id`, `spotId`, `name`, `address` | | `/week/mine`에서만 온다. `type`은 `BOOKMARK`, `id`는 북마크 ID다. `spotId`는 명소를 저장한 북마크에만 있다 |
| `latitude`, `longitude` | number | |
| `skyBrightness`, `skyClass`, `skyRank` | | 밤 추천과 같다 |
| `forecastSource`, `forecastIssuedAt` | string | 밤 추천과 같다. 예보를 받지 못한 장소에서는 빠진다 |
| `nights` | array | 날짜순의 밤. 예보가 닿지 않는 밤은 빠져서 7개보다 적을 수 있다. 예보를 받지 못한 장소는 빈 배열이다 |

`nights`의 항목은 `date`와, 밤 추천의 장소 항목에 있는 `level`, `pattern`, `notes`, `bestWindow`, `moonBrightness`,
`moon`, `hours`다. 뜻과 빠지는 조건도 같다.

```
GET /api/v1/recommendations/week?lat=37.62633&lon=128.73942
```

```json
{
  "success": true,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "startDate": "2026-11-09",
    "dates": ["2026-11-09", "2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13", "2026-11-14", "2026-11-15"],
    "criteria": { "minClearHours": 3, "preferBefore": "02:00" },
    "places": [
      {
        "latitude": 37.62633,
        "longitude": 128.73942,
        "skyBrightness": 3.11,
        "skyClass": "RURAL",
        "skyRank": 22,
        "forecastSource": "OPEN_METEO",
        "forecastIssuedAt": "2026-11-23T15:00:00+09:00",
        "nights": [
          {
            "date": "2026-11-09",
            "level": "STRONG",
            "pattern": "CLEAR_ALL_NIGHT",
            "notes": [],
            "bestWindow": {
              "start": "2026-11-09T19:00:00+09:00",
              "end": "2026-11-10T06:00:00+09:00",
              "hours": 11
            },
            "moonBrightness": 0.0,
            "moon": {
              "illuminatedFraction": 0.0,
              "waxing": true,
              "rise": "2026-11-10T07:54:00+09:00",
              "set": "2026-11-09T16:52:00+09:00"
            },
            "hours": [
              {
                "time": "2026-11-09T18:00:00+09:00",
                "dark": false,
                "clear": false,
                "cloudCover": 0,
                "cloudCoverLow": 0,
                "cloudCoverMid": 0,
                "cloudCoverHigh": 0,
                "relativeHumidity": 60,
                "moonUp": false,
                "score": 0
              }
            ]
          },
          {
            "date": "2026-11-10",
            "level": "NO",
            "pattern": "CLOUDY_ALL_NIGHT",
            "notes": [],
            "moonBrightness": 0.0,
            "moon": {
              "illuminatedFraction": 0.02,
              "waxing": true,
              "rise": "2026-11-11T08:55:00+09:00",
              "set": "2026-11-10T17:28:00+09:00"
            },
            "hours": []
          }
        ]
      }
    ]
  }
}
```

`WeekOutlookApiTest`가 고정한 예보로 받은 응답이다. `nights`는 둘만 남기고 `hours`는 하나만 남기거나 비웠다.
실제로는 밤 7개, 밤마다 시간 칸 13개가 온다. `forecastIssuedAt`이 밤보다 뒤인 것은 테스트의 모델 실행 시각이
고정값이기 때문이다.

먼 날의 예보는 가까운 날보다 자주 바뀐다. 응답에는 신뢰도 값이 없으므로, 화면에서 뒤쪽 밤은 달라질 수 있다고
알린다. 예보 출처는 이 화면에도 적는다.

### 한 좌표의 하늘 밝기

```
GET /api/v1/recommendations/sky?lat=37.62633&lon=128.73942
```

지도에서 고른 자리가 얼마나 어두운지 보여 줄 때 쓴다. 예보를 받지 않고 서버 메모리의 격자만 읽어서 가볍다.
`lat`, `lon`은 필수이고, 빠지거나 범위를 벗어나면 400이다.

```json
{
  "latitude": 37.62633,
  "longitude": 128.73942,
  "skyBrightness": 3.11,
  "skyClass": "RURAL",
  "skyRank": 22,
  "litSurroundings": false
}
```

위는 `data`의 내용이다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `latitude`, `longitude` | number | 요청한 좌표 |
| `skyBrightness`, `skyClass`, `skyRank` | number, string, integer | 밤 추천과 같다 |
| `litSurroundings` | boolean | 둘레 1km가 가로등과 건물 불빛으로 밝은지. `true`면 밤 추천에서 맑아도 "별 보기 아쉬워요"까지만 준다 |

자료가 있는 범위(한국 둘레) 밖이면 하늘 값이 모두 빠지고 좌표만 온다. 오류가 아니라 200이다. 직접 보이는 불빛
(`LIGHTS_IN_VIEW`)은 명소마다 미리 계산한 값이라 이 API에는 없다.

### 한 좌표의 달력

```
GET /api/v1/recommendations/moon?lat=37.5665&lon=126.978
```

오늘 밤부터 6주(42밤) 동안 밤마다 달 모양, 뜨고 지는 시각, 달 없이 어두운 시간을 준다. 예보를 보지 않고 해와 달의
위치만 계산하므로 예보가 닿지 않는 날의 계획에 쓴다. 날씨와 하늘 밝기는 들어 있지 않다. `lat`, `lon`은 필수이고, 빠지거나
범위를 벗어나면 400이다. 오늘 밤은 밤 추천과 같이 새벽 4시 전이면 전날이다.

```json
{
  "latitude": 37.5665,
  "longitude": 126.978,
  "startDate": "2026-11-09",
  "nights": [
    {
      "date": "2026-11-09",
      "moon": {
        "illuminatedFraction": 0.0,
        "waxing": true,
        "rise": "2026-11-10T08:01:00+09:00",
        "set": "2026-11-09T16:59:00+09:00"
      },
      "dark": { "start": "2026-11-09T18:25:00+09:00", "end": "2026-11-10T06:07:00+09:00" },
      "moonless": [{ "start": "2026-11-09T18:25:00+09:00", "end": "2026-11-10T06:07:00+09:00" }],
      "moonlessMinutes": 702
    }
  ]
}
```

위는 `data`의 내용이고, 실제로는 밤이 42개 온다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `startDate` | string | 첫 밤의 날짜 |
| `nights[].date` | string | 밤이 시작하는 날짜 |
| `nights[].moon` | object | 밤 추천의 `moon`과 같다 |
| `nights[].dark` | object | 하늘이 어두운 구간(태양 고도 -12° 이하). 밤 추천에서 `dark`가 `true`인 칸과 같은 기준이다. 그만큼 어두워지지 않는 곳과 날이면 빠진다 |
| `nights[].moonless` | array | `dark` 가운데 달이 지평선 아래에 있는 구간. 시간순이다. 달이 밤새 떠 있으면 빈 배열이고, 달이 밤중에 잠깐 떴다 지면 둘이다 |
| `nights[].moonlessMinutes` | integer | `moonless` 구간 길이의 합(분). 응답에 적힌 시각으로 셈한 값이다 |

`moonless`의 끝이 달이 뜨고 지는 때이면 `moon.rise`, `moon.set`과 같은 시각이 온다. 가는 초승달도 떠 있으면 있는
것으로 센다. 달이 얼마나 밝은지는 `moon.illuminatedFraction`으로 따로 보여 준다.

## 7. 관측지

```
GET /api/v1/spots
```

명소는 두 종류다. `OPEN_SITE`는 밤에 자유롭게 가서 맨눈으로 보는 관측지이고 밤 추천의 후보가 된다.
`FACILITY`는 운영 시간에 프로그램으로 망원경을 보는 천문대와 과학관이고 밤 추천에는 나오지 않는다.

| 쿼리 | 타입 | 설명 |
|---|---|---|
| `lat`, `lon` | number | 중심 좌표. 둘 다 빼면 전체 명소가 온다 |
| `radius` | integer, 10~500 | 반경(km). 좌표만 보내고 빼면 20km로 찾는다 |
| `kind` | string | `OPEN_SITE` 또는 `FACILITY`. 빼면 둘 다 온다. 모르는 값이면 400이다 |

`data`는 배열이고, 조건에 맞는 명소가 없으면 빈 배열이다. 좌표를 보내면 가까운 순으로 온다. 페이지 나눔은 없다.
명소는 41곳이고 관측지가 24곳, 천문대와 과학관이 17곳이다. 목록에서 뺀 곳은 오지 않는다.
미시령 옛길, 벗고개, 운여해변은 차를 대는 자리를 지도에서 특정하지 못해 좌표가 대략의 위치다. `guide`에 그렇게
적혀 있고, 자리를 설명한 글이 `links`의 첫 번째에 있다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | integer | 명소 ID. 북마크의 `spotId`, 밤 추천의 `spotId`와 같은 번호다 |
| `title` | string | 이름 |
| `kind` | string | `OPEN_SITE` 또는 `FACILITY` |
| `viewpoint` | string | 실제로 차를 대고 보는 지점의 이름. 좌표가 가리키는 곳이다. `FACILITY`에서는 빠진다 |
| `address` | string | 번지를 확인하지 못한 곳은 동리까지만 적고 괄호 안에 지점을 적었다 |
| `latitude`, `longitude` | number | `OPEN_SITE`는 `viewpoint`의 좌표, `FACILITY`는 건물의 좌표다 |
| `description` | string | 어떤 곳인지 한두 문장 |
| `guide` | string | 가는 법과 주차. `FACILITY`에서는 운영 시간과 예약 방법이다 |
| `caution` | string | 계절 폐쇄, 조명, 통제 같은 주의 사항. 없으면 빠진다 |
| `links` | array | 참고 링크. 항목은 `title`(그 글의 제목), `url`, `imageUrl`(그 글의 대표 이미지, 없으면 빠진다)이다. 가는 길과 서는 자리를 더 자세히 적은 기사, 관광 안내, 후기이고 `FACILITY`에서는 공식 페이지다. 적어 둔 순서대로 오고, 없으면 빠진다 |
| `points` | array | 이 명소 안에서 서서 보는 자리들. 항목은 `name`, `access`(어떻게 가는지와 어떤 자리인지), `latitude`, `longitude`다. 첫 항목이 차를 대는 곳이고 나머지는 거기서 차로 조금 가거나 30분 안쪽으로 걸어가는 곳이다. 좌표는 아는 자리에만 있다. 자리가 하나뿐인 명소에서는 빠진다 |
| `videos` | array | 이 명소에서 별을 본 유튜브 영상. 항목은 `title`(영상 제목), `channel`(올린 채널), `url`(여는 주소), `thumbnailUrl`(320×180 미리보기 그림), `shortForm`(쇼츠인지)이다. 골라 둔 순서대로 최대 4개가 오고, 영상을 찾지 못한 명소와 시설에서는 빠진다 |
| `bortleScale` | integer | 지금은 모든 명소에서 빠져 있다. 하늘이 얼마나 어두운지는 밤 추천의 `skyBrightness`를 본다 |
| `isParkingAvailable`, `isRestroomAvailable`, `isCarAccess` | boolean | 주차, 화장실, 차량 접근. 확인하지 못한 값은 빠진다 |
| `thumbnailImage` | string | 사진 주소. 지금은 모든 명소에서 빠져 있다 |

```
GET /api/v1/spots?kind=OPEN_SITE
```

```json
{
  "success": true,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "id": 1,
      "title": "강릉 안반데기",
      "kind": "OPEN_SITE",
      "viewpoint": "멍에전망대 주차장",
      "address": "강원특별자치도 강릉시 왕산면 대기리 (멍에전망대)",
      "latitude": 37.62633,
      "longitude": 128.73942,
      "description": "해발 1,100m 고랭지 배추밭 위의 고원이에요. 맨눈으로 은하수가 보이는 곳으로 가장 널리 알려져 있어요.",
      "guide": "멍에전망대 주차장에 대요. 40면쯤이라 주말에는 모자라요. 무료이고 주차장에 화장실이 있어요. 올라가는 산길이 좁고 가팔라요.",
      "caution": "농로에는 주차와 차박이 금지예요. 주민들이 배추를 실어 내는 길이라 주차장만 써 주세요. 텐트와 취사도 안 돼요.",
      "links": [
        {
          "title": "해발 1,100m에서 맨눈으로 은하수 보는 겨울 명소",
          "url": "https://www.ardentnews.co.kr/news/articleView.html?idxno=10300",
          "imageUrl": "https://cdn.ardentnews.co.kr/news/thumbnail/202601/10300_45700_5624_v150.jpg"
        },
        {
          "title": "겨울 차박족으로 몸살 앓는 별의 성지 안반데기",
          "url": "https://v.daum.net/v/Q16NWPhVPS",
          "imageUrl": "https://img1.daumcdn.net/thumb/S800x400/?fname=https://t1.daumcdn.net/news/202311/19/traveltoktok/20231119091009254hsms.jpg"
        }
      ],
      "videos": [
        {
          "title": "4K) 안반데기 3월 은하수 타임랩스",
          "channel": "Han",
          "url": "https://www.youtube.com/watch?v=xEfv8BDCpgw",
          "thumbnailUrl": "https://i.ytimg.com/vi/xEfv8BDCpgw/mqdefault.jpg",
          "shortForm": false
        }
      ],
      "isParkingAvailable": true,
      "isRestroomAvailable": true,
      "isCarAccess": true
    }
  ]
}
```

`data`는 하나만 남기고 줄였다. `description`, `guide`, `caution`은 화면에 그대로 보여 주는 문장이다.
`links`는 남의 글이라 언제든 사라질 수 있다. 새 창으로 열고, 링크가 없는 곳도 있다는 것을 전제로 그린다.
`imageUrl`은 그 글이 대표 이미지(og:image)로 내건 주소이고 남의 서버에 있는 파일이다. 링크의 미리보기로만 쓰고,
리퍼러를 보내지 않고 부르며(`referrerpolicy="no-referrer"`), 받지 못하면 이미지 없이 그린다.

`videos`는 그곳에서 별을 본 유튜브 영상이다(위 예시는 셋 가운데 하나만 남겼다). 제목과 채널 이름은 유튜브가 알려 준
그대로이고, 영상은 `url`을 새 창으로 열어 유튜브에서 보게 한다. `thumbnailUrl`은 유튜브의 그림 서버(`i.ytimg.com`)에
있다. 영상이 지워지면 이 주소는 오류 대신 120×90의 빈 그림을 돌려주므로, 받은 그림의 너비가 120 이하이면 그 영상은
그리지 않는다.

같은 장소의 이름이 여기서는 `title`, 밤 추천과 북마크에서는 `name`이다.

## 8. 지명 검색

이름으로 좌표를 찾거나 좌표가 어느 지역인지 알아낸다. 웹은 지도의 검색창, 출발 위치 정하기, 지도에서 고른 위치에
이름 붙이기에 쓴다. 한국 안만 찾는다. 로그인이 필요 없다.

### 이름으로 찾기

```
GET /api/v1/geocode/search?q=화악산
```

| 쿼리 | 타입 | 설명 |
|---|---|---|
| `q` | string, 2~50자 | 찾을 이름. 필수 |

`data`는 배열이고 최대 8개다. 찾은 곳이 없으면 빈 배열이다. 이름뿐 아니라 지번이나 도로명 주소로도 찾을 수 있다.

서버는 두 곳에 묻는다. OpenStreetMap은 시, 군, 읍면동과 산을 잘 알고, 카카오 로컬은 전망대, 주차장, 공원 같은 국내
장소와 주소를 잘 안다. 순서는 OpenStreetMap의 지역과 산 세 곳까지, 카카오가 찾은 주소 두 곳까지, 카카오의 목적지
(관광지, 공원, 역, 주차장), OpenStreetMap의 남은 지역과 산, 카카오의 나머지(숙소, 가게), OpenStreetMap의 나머지(역,
정류장, 도로, 학교)다. 같은 이름이 1km 안에 여럿이면 앞의 하나만 온다. 한쪽이 답하지 않으면 다른 쪽의 결과만 오고,
둘 다 답하지 않을 때만 503이다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `name` | string | 이름. 주소로 찾은 결과는 정리된 전체 주소가 이름이다 |
| `address` | string | 같은 이름을 구분하는 행정구역. 모르면 빈 문자열이다 |
| `latitude`, `longitude` | number | |
| `source` | string | 결과의 출처. `OSM`(OpenStreetMap) 또는 `KAKAO`(카카오 로컬). 목록에 출처를 적는 데 쓴다 |

```json
{
  "success": true,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    { "name": "화악산", "address": "경기도 가평군 북면", "latitude": 37.9946674, "longitude": 127.5034314, "source": "OSM" },
    { "name": "화악산 출입로", "address": "경기 가평군 북면 화악리", "latitude": 37.9918266, "longitude": 127.5239149, "source": "KAKAO" }
  ]
}
```

입력하는 도중에 불러도 된다. 다만 글자마다 부르지 말고 입력이 0.3초쯤 멈췄을 때 부른다. OpenStreetMap의 결과는
서버가 하루 동안 기억하고, 카카오의 결과는 기억하지 않고 매번 새로 받는다.

결과를 보여 주는 화면에는 출처를 적는다. `OSM`이 섞여 있으면 "© OpenStreetMap contributors", `KAKAO`가 섞여 있으면
카카오에서 온 결과라는 것을 적는다.

### 좌표의 지역 이름

```
GET /api/v1/geocode/reverse?lat=37.623&lon=128.739
```

| 쿼리 | 타입 | 설명 |
|---|---|---|
| `lat`, `lon` | number | 둘 다 필수 |

```json
{
  "success": true,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { "name": "강릉시 왕산면" }
}
```

`name`은 가장 작은 행정구역 둘이다. 바다 한가운데처럼 10km 안에 아무것도 없으면 `name`이 빠지고 `data`는 빈
객체다. 서버는 좌표를 소수 셋째 자리(100m쯤)로 줄여서 찾는다.

### 오류

| 상태 | 경우 |
|---|---|
| 400 | `q`가 없거나 2자보다 짧거나 50자보다 길다. `lat`, `lon`이 없거나 범위를 벗어났다 |
| 429 | 서버가 지명 데이터 소스에 보내는 요청이 한도에 닿았다. 1초쯤 뒤에 다시 보낸다 |
| 503 | 지명 데이터 소스가 답하지 않는다 |

검색이 실패해도 추천과 지도는 그대로 쓸 수 있어야 한다. 검색창에만 실패를 알리고 나머지 화면은 막지 않는다.

### 출처 표기

지명 데이터는 OpenStreetMap이고 [Photon](https://photon.komoot.io/)으로 찾는다. OpenStreetMap은 ODbL이라
검색 결과를 보여 주는 화면에 "© OpenStreetMap contributors"를 적어야 한다. OpenStreetMap 기반 지도를 함께
보여 주고 있어 지도에 이미 이 표기가 있으면 따로 더 적지 않아도 된다.

## 9. 구름 지도

지도에 구름이 어디에 끼는지 그리는 데 쓰는 격자 예보다. 한국과 주변 바다(위도 32.6°~39.0°, 경도 124.2°~131.0°)를
0.4° 간격으로 나눈 17줄 18칸, 306점의 시간별 총운량을 준다. 로그인이 필요 없다.

```
GET /api/v1/clouds/nights/{date}
```

`{date}`는 밤이 시작하는 날짜(`yyyy-MM-dd`)다. 밤 추천 응답의 `date`를 그대로 넣으면 된다. 그날 저녁 5시부터
다음 날 아침 8시 전까지 한 시간 간격으로 최대 15개가 온다. 예보가 닿지 않는 날짜면 오류가 아니라 `hours`가
빈 배열이다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `date` | string | 요청한 날짜 |
| `source` | string | 예보 출처. 지금은 `OPEN_METEO`뿐이다 |
| `issuedAt` | string | 예보 모델이 계산을 시작한 시각(UTC). 확인하지 못했으면 빠진다 |
| `grid.south`, `grid.west` | number | 남서쪽 모서리 격자점의 위도와 경도 |
| `grid.step` | number | 줄과 칸 사이의 간격(도) |
| `grid.rows`, `grid.columns` | integer | 줄(남→북)과 칸(서→동)의 수 |
| `hours[].time` | string | 이 시각부터 1시간. 밤 추천의 `hours[].time`과 같은 형식이다 |
| `hours[].cloudCover` | integer[] | 총운량(%). 길이는 `rows × columns`이고 남쪽 줄부터, 줄 안에서는 서쪽부터 적는다 |

`cloudCover[i]`의 좌표는 위도 `south + step × floor(i / columns)`, 경도 `west + step × (i % columns)`다.

```json
{
  "success": true,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "date": "2026-11-24",
    "source": "OPEN_METEO",
    "issuedAt": "2026-11-23T06:00:00Z",
    "grid": { "south": 32.6, "west": 124.2, "step": 0.4, "rows": 17, "columns": 18 },
    "hours": [
      { "time": "2026-11-24T17:00:00+09:00", "cloudCover": [96, 2, 9] }
    ]
  }
}
```

`hours`는 하나만 남기고 `cloudCover`도 앞의 셋만 남겼다. 실제로는 시각마다 306개가 온다.

쓸 때 알아 둘 것:

- 격자 간격이 약 40km다. 구름의 모양이 아니라 어느 쪽이 흐린지, 밤사이 구름대가 어디로 가는지를 보는 용도다.
  그릴 때는 점 사이를 선형으로 잇기만 한다. 이웃 점과 평균을 내 고르게 하면 구름대의 경계가 한 칸씩 번져서
  맑은 곳까지 구름이 덮인 것처럼 보인다.
- 장소의 판정에는 쓰지 않는다. 장소가 맑은지는 밤 추천 응답의 그 장소 값으로 본다. 두 값은 같은 모델에서
  나오지만 뽑은 지점이 달라 어긋날 수 있다.
- 서버는 모델이 새로 돌 때(6시간마다)만 격자를 다시 받는다. 그 사이에는 여러 번 불러도 같은 값이다.
- 웹 메르카토르 지도에 그림으로 얹을 때는 그림의 세로축을 위도가 아니라 메르카토르 y에 비례하게 그려야 한다.
- 받지 못하면 503이다. 구름 그림 없이도 지도와 추천은 쓸 수 있어야 한다.
- 출처 표기는 밤 추천과 같다([6장](#예보-출처-표기)). Open-Meteo, CC BY 4.0이다.

## 10. 북마크

화면에서는 "내 장소"라고 부른다. 모두 액세스 토큰이 필요하다. 두 가지가 있다.

- `SPOT`: 서비스에 등록된 명소를 저장한 것. 좌표와 주소는 명소의 값을 따른다.
- `CUSTOM`: 직접 찍은 위치. 좌표와 주소를 저장할 때 보낸다.

### 북마크 객체

| 필드 | 타입 | 설명 |
|---|---|---|
| `bookmarkId` | integer | |
| `type` | string | `SPOT` 또는 `CUSTOM` |
| `spotId` | integer 또는 null | `CUSTOM`이면 `null` |
| `name` | string | 사용자가 붙인 이름 |
| `latitude`, `longitude` | number | |
| `address` | string | |
| `memo` | string 또는 null | |

### 목록

```
GET /api/v1/bookmarks
```

`data`는 북마크 객체의 배열이다. 없으면 빈 배열이다.

### 추가

```
POST /api/v1/bookmarks
```

| 필드 | 타입 | `SPOT` | `CUSTOM` | 설명 |
|---|---|---|---|---|
| `type` | string | 필수 | 필수 | |
| `name` | string | 필수 | 필수 | 50자까지. 명소 이름을 자동으로 채워 주지 않는다 |
| `spotId` | integer | 필수 | 쓰지 않음 | |
| `latitude`, `longitude` | number | 쓰지 않음 | 필수 | |
| `address` | string | 쓰지 않음 | 필수 | |
| `memo` | string | 선택 | 선택 | 500자까지 |

```json
{ "type": "SPOT", "spotId": 1, "name": "안반데기" }
```

```json
{
  "type": "CUSTOM",
  "name": "우리집 옥상",
  "latitude": 37.55,
  "longitude": 126.91,
  "address": "서울 마포구",
  "memo": "남쪽이 트여 있다"
}
```

성공하면 `data`에 만들어진 북마크 객체가 온다.

| 상태 | 경우 |
|---|---|
| 400 | `type`이나 `name`이 없다. `SPOT`에 `spotId`가 없다(`spotValid`). `CUSTOM`에 좌표가 없다(`customValid`) |
| 400 | `CUSTOM`에 `address`가 없다. 이때는 `data` 없이 "북마크 타입과 데이터가 일치하지 않습니다."만 온다 |
| 400 | `type`이 `SPOT`, `CUSTOM`이 아니다 |
| 404 | `spotId`의 명소가 없다 |
| 409 | 이미 저장한 명소다. 같은 좌표의 `CUSTOM`은 여러 번 저장할 수 있다 |

### 수정

```
PATCH /api/v1/bookmarks/{bookmarkId}
```

```json
{ "name": "옥상", "memo": "난간 조심" }
```

이름과 메모만 바꿀 수 있다. `name`은 필수다. `memo`를 빼고 보내면 메모가 지워진다. 그러니 메모를 그대로 두려면
기존 값을 함께 보낸다. 성공하면 `data`에 바뀐 북마크 객체가 온다.

남의 북마크면 403, 없는 북마크면 404다.

### 삭제

```
DELETE /api/v1/bookmarks/{bookmarkId}
```

성공하면 `data` 없이 200이다. 남의 북마크면 403, 없는 북마크면 404다.

## 11. 회원

### 내 정보

```
GET /api/v1/members/me
```

액세스 토큰이 필요하다.

```json
{ "success": true, "status": 200, "message": "요청이 성공적으로 처리되었습니다.", "data": { "memberId": 1, "nickname": "별지기" } }
```

이메일은 응답에 없다. 비밀번호 변경, 탈퇴 API는 아직 없다.

### 닉네임 수정

```
PATCH /api/v1/members/me
```

액세스 토큰이 필요하다. 응답은 내 정보와 같은 모양이다.

```json
{ "nickname": "밤하늘 산책" }
```

- 앞뒤 공백을 떼고 2~16자다. 한글, 영문, 숫자, 밑줄과 낱말 사이의 공백 하나만 받는다. 저장되는 값은 앞뒤 공백을 뗀 것이다.
- 자모로 풀어 온 한글(NFD)은 음절로 모아서 센다.
- 다른 회원이 쓰는 닉네임은 받지 않는다. 대소문자는 가리지 않아서 `Star`가 있으면 `star`도 409다.
  자기 닉네임의 대소문자만 바꾸는 것은 된다.
- 규칙에 어긋나거나 `nickname`이 빠지면 400, 겹치면 409이고 둘 다 `data` 없이 메시지만 온다. 닉네임은 바뀌지 않는다.

```json
{ "success": false, "status": 400, "message": "닉네임은 2~16자의 한글, 영문, 숫자, 밑줄로 쓰고 낱말 사이에는 공백을 하나만 둘 수 있습니다." }
{ "success": false, "status": 409, "message": "이미 쓰는 닉네임입니다." }
```

규칙과 겹침은 정할 때만 본다. 이 규칙이 생기기 전에 가입해서 규칙에 어긋나거나 다른 회원과 겹치는 닉네임은 그대로 두고,
`GET /api/v1/members/me`도 그 값을 준다. 한 번 바꾼 닉네임은 다시 로그인해도 돌아가지 않는다.
액세스 토큰에는 닉네임이 없으므로 바꾼 뒤 토큰을 다시 받을 필요가 없다.

선정적이거나 광고인 닉네임은 운영자가 알리지 않고 기본 닉네임으로 되돌릴 수 있다. 앱은 닉네임을 정하는 화면에 이를 알린다.

### 닉네임 중복 검사

```
GET /api/v1/members/exists/nickname?nickname=별지기
```

로그인 없이 부를 수 있다. `data.validated`가 `true`면 쓸 수 있는 닉네임이고, `false`면 다른 회원이 쓰고 있다.
앞뒤 공백을 떼고, 대소문자를 가리지 않고 본다. 로그인한 채로 부르면 자기 닉네임은 `true`다. 규칙에 어긋나면
닉네임 수정과 같은 400이다. 가입이나 수정 요청도 같은 검사를 하므로, 이 API는 입력하는 동안 미리 알려 주는 용도다.

### 닉네임 초기화 (관리자)

```
POST /api/v1/admin/members/{memberId}/nickname/reset
```

그 회원의 닉네임을 아무도 쓰지 않는 기본 닉네임(`별지기4821` 꼴)으로 바꾸고, 바뀐 회원 정보를 내 정보와 같은 모양으로
돌려준다. 액세스 토큰의 역할은 늘 `USER`라서 관리자인지는 DB의 회원 역할(`member.role = 'ADMIN'`)로 본다. 관리자가
아니면 403, 없는 회원이면 404다.

### 이메일 중복 검사

```
GET /api/v1/members/exists/email?email=user@example.com
```

로그인 없이 부른다. `data.validated`가 `true`면 쓸 수 있는 이메일이고, `false`면 이미 가입된 이메일이다.
자체 가입 회원의 이메일만 본다. 소셜 로그인 회원이 쓰는 이메일로도 자체 가입할 수 있고, 두 계정은 따로다.
경로의 `exists`와 뜻이 반대라는 점에 주의한다. 이메일 형식은 검사하지 않는다. `email`이 빠지면 400이다.

## 12. 인증 엔드포인트

### 가입 인증 코드 받기

```
POST /api/v1/auth/email-code
```

```json
{ "email": "user@example.com" }
```

그 이메일로 여섯 자리 인증 코드를 보낸다. 성공하면 `data` 없이 200이다. 코드는 10분 동안 쓸 수 있고, 다시 받으면
앞의 코드는 못 쓴다. 이메일의 대소문자는 가리지 않는다.

| 상태 | 언제 |
|---|---|
| 400 | 이메일 형식이 아닐 때 (`data.email`에 사유) |
| 409 | 이미 가입된 이메일일 때. 메일을 보내지 않는다 |
| 429 | 같은 이메일로 1분 안에 다시 받거나, 그날 다섯 번을 넘겼을 때 |
| 503 | 메일을 보내지 못했거나 서비스의 하루 발송량을 넘겼을 때. 바로 다시 받을 수 있다 |

### 회원가입

```
POST /api/v1/auth/register
```

| 필드 | 설명 |
|---|---|
| `email` | 이메일 형식 |
| `password` | 8자 이상. 영문자, 숫자, 특수문자(`!@#$%^&*`)가 하나씩은 있어야 한다 |
| `nickname` | 닉네임 수정과 같은 규칙. 다른 회원과 겹치면 안 된다 |
| `emailCode` | 그 이메일로 받은 여섯 자리 인증 코드 |

성공하면 로그인한 것과 같은 응답이 온다. 따로 로그인할 필요가 없다. 이미 가입된 이메일이면 409다.
닉네임이 비었으면 다른 필드처럼 `data.nickname`에 사유가 담긴 400이고, 규칙에 어긋나면 `data` 없는 400이다.
다른 회원이 쓰는 닉네임이면 이메일과 같은 409인데 `message`가 다르다. 상태 코드만으로 둘을 가릴 수 없으므로,
보내기 전에 닉네임 중복 검사를 불러 닉네임 쪽을 먼저 걸러 준다.

인증 코드는 다른 검사를 모두 지난 뒤에 확인하고, 맞으면 지운다(한 번만 쓴다). 그래서 닉네임이 겹쳐 실패했으면 같은
코드로 고쳐서 다시 보낼 수 있다. 코드를 받지 않았거나 10분이 지났으면 "인증 코드를 다시 받아 주세요.", 틀리면
"인증 코드가 맞지 않습니다."인 400이다. 한 코드로 다섯 번 틀리면 코드가 지워져 다시 받아야 한다.

비밀번호 규칙은 정규식 `^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*]).{8,}$`이다. 보내기 전에 앱에서 같은 식으로
먼저 걸러 준다.

### 로그인

```
POST /api/v1/auth/login
```

```json
{ "email": "user@example.com", "password": "starry-night1!" }
```

```json
{
  "success": true,
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { "accessToken": "eyJhbGciOiJIUzI1NiJ9..." }
}
```

리프레시 토큰은 같은 응답의 `Set-Cookie` 헤더에 있다. 이메일이 없거나 비밀번호가 틀리면 401이고 두 경우의
응답이 같다.

### 재발급

```
POST /api/v1/auth/reissue
```

본문은 없다. 리프레시 토큰을 쿠키나 `Authorization-Refresh` 헤더로 보낸다. 응답은 로그인과 같고, 새 리프레시
토큰이 `Set-Cookie`로 온다. 헤더로 보내는 앱은 이 값으로 저장한 토큰을 바꿔야 한다. 토큰이 없거나, 이미 썼거나,
만료됐으면 401이다.

### 로그아웃

```
POST /api/v1/auth/logout
```

본문은 없다. 리프레시 토큰을 재발급과 같은 방식으로 보낸다. 서버에서 그 토큰을 없애고 쿠키를 지우는
`Set-Cookie`를 내려 준다. 토큰 없이 불러도 200이다.

## 13. 알려진 한계

고칠 예정이거나 아직 정하지 않은 것들이다. 여기 적힌 동작에 기대어 만들지 않는다.

- 오류 응답에 기계가 읽는 코드가 없다. 넣게 되면 기존 필드는 그대로 두고 `code`를 더하는 방식이 된다.
- 500 응답의 `message`에 내부 예외 문구가 그대로 실린다.
- `CUSTOM` 북마크는 주소가 필수다. 현재 위치를 주소 없이 저장할 수 없고, 빠졌을 때의 오류에 `data`가 없다.
- 북마크를 수정할 때 `memo`를 빼면 지워진다.
- 네이티브 앱용 소셜 로그인 흐름이 없다.
- 닉네임 변경, 비밀번호 변경, 회원 탈퇴 API가 없다.
- 밤 추천은 등록된 명소와 북마크만 평가한다. 임의의 좌표를 평가하는 API는 없다.
- 이동 시간은 직선거리로 어림한 값이다.
- 운영 서버가 `dev` 브랜치를 그대로 따라간다. 깨지는 변경은 [decisions.md](./decisions.md)에 날짜와 함께 적는다.

## 14. 문서를 고칠 때

이 문서의 응답 예시와 상태 코드는 아래 통합 테스트가 고정하고 있다. API를 바꾸면 테스트와 이 문서를 같은
커밋에서 고친다.

| 범위 | 테스트 |
|---|---|
| 인증 | [`AuthApiTest`](../src/test/java/xyz/ncookie/stargazer/domain/member/controller/AuthApiTest.java) |
| 회원 | [`MemberApiTest`](../src/test/java/xyz/ncookie/stargazer/domain/member/controller/MemberApiTest.java) |
| 북마크 | [`BookmarkApiTest`](../src/test/java/xyz/ncookie/stargazer/domain/bookmark/controller/BookmarkApiTest.java) |
| 관측지 | [`ObservationSpotApiTest`](../src/test/java/xyz/ncookie/stargazer/domain/spot/controller/ObservationSpotApiTest.java) |
| 구름 지도 | [`CloudMapApiTest`](../src/test/java/xyz/ncookie/stargazer/domain/weather/controller/CloudMapApiTest.java) |
| 지명 검색 | [`GeocodeApiTest`](../src/test/java/xyz/ncookie/stargazer/domain/geocode/controller/GeocodeApiTest.java) |
| 밤 추천 | [`NightRecommendationApiTest`](../src/test/java/xyz/ncookie/stargazer/domain/recommend/controller/NightRecommendationApiTest.java) |
| 이번 주 밤 | [`WeekOutlookApiTest`](../src/test/java/xyz/ncookie/stargazer/domain/recommend/controller/WeekOutlookApiTest.java) |
| 한 좌표의 하늘 밝기 | [`SkyApiTest`](../src/test/java/xyz/ncookie/stargazer/domain/recommend/controller/SkyApiTest.java) |
| 없는 경로, 틀린 메서드 | [`FrameworkErrorStatusTest`](../src/test/java/xyz/ncookie/stargazer/global/exception/FrameworkErrorStatusTest.java) |

관련 문서는 다음과 같다.

- [MOBILE_DEVELOPER_GUIDE.md](./MOBILE_DEVELOPER_GUIDE.md): 네이티브 앱에서 챙길 것
- [SECURITY_AUTH_GUIDE.md](./SECURITY_AUTH_GUIDE.md): 인증 구조를 그렇게 잡은 이유
- [SCORING.md](./SCORING.md): 등급과 점수의 기준, 자료 출처
- [decisions.md](./decisions.md): API가 바뀐 이력과 이유
