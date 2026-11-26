# 🌌 Stargazer - 별 관측 적합도 분석 서비스

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

> **"오늘 밤, 별 보러 가도 될까?"**  
> 위치 기반 기상·천문·광해 데이터를 종합 분석하여 최적의 별 관측 시간과 장소를 추천하는 **REST API 백엔드** 서비스입니다.

---

## 📖 프로젝트 개요

단순히 날씨가 맑다고 별이 잘 보이는 것은 아닙니다. 달의 밝기(월령), 주변 광해, 대기 투명도 등이 복합적으로 작용합니다.  
별볼일 백엔드는 시간별 예보(**Open-Meteo**, 장애 시 **MET Norway**), 위성 야간광(**VIIRS**)으로 계산한 **하늘 밝기**, 해와 달의 위치를 묶어 **오늘 밤 어디서 언제 별이 잘 보이는지**를 장소마다 평가하고, **회원·북마크·관측지·지명 검색·구름 지도** 기능을 지원합니다.

### 🎯 대상
- 웹/모바일 클라이언트를 위한 **REST API 서버**
- 별 관측·드라이브·스마트폰 천체 촬영에 관심 있는 일반 사용자

---

## 🚀 구현된 핵심 기능

| 구분 | 기능 | 설명 |
|------|------|------|
| **밤 추천** | 오늘 밤, 날짜별 | 기본 명소와 내 북마크를 밤마다 평가해 맑은 시간대, 등급, 이동 시간과 함께 순서대로 |
| **밤 추천** | 이번 주 | 한 장소(임의 좌표 또는 북마크)의 7일 밤을 한 번에 |
| **지도** | 구름 격자 | 한국을 덮는 격자의 시간별 구름량 |
| **지도** | 지명 검색 | 이름으로 장소 찾기, 좌표의 지역 이름 |
| **관측지** | 주변 명소 조회 | 위도/경도·반경(10~500km) 기준 관측지 목록 (공간 인덱스) |
| **인증** | 로그인/회원가입 | 이메일·비밀번호 기반, JWT Access Token + Refresh Token(쿠키) |
| **인증** | 토큰 재발급·로그아웃 | RTR(Refresh Token Rotation), HttpOnly 쿠키 |
| **회원** | 내 정보·이메일 중복 검사 | `GET /me`, `GET /exists/email` |
| **북마크** | CRUD | 관측지(SPOT) 또는 사용자 정의(CUSTOM) 위치 저장·수정·삭제 |

---

## 🌟 추천 기준

밤마다 해가 진 뒤의 어두운 시간을 1시간 단위로 보고, 구름이 적게 이어지는 구간과 하늘 밝기, 달, 습도로 등급을 정합니다.
기준값과 그 출처는 [docs/SCORING.md](docs/SCORING.md)에, 그렇게 정한 이유는 [docs/decisions.md](docs/decisions.md)에 있습니다.

---

## 🛠️ 기술 스택

| 구분 | 기술 |
|------|------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 4.x, Spring Web MVC, Spring Data JPA, Spring Security |
| **인증** | JWT (Access/Refresh), HttpOnly Cookie, Redis(Refresh Token 저장) |
| **DB** | MySQL 8.0 (Spatial), Redis (캐시·세션) |
| **외부 API** | Open-Meteo, MET Norway, Photon, 카카오 로컬 |
| **기타** | SunCalc(천문 계산), Hibernate Spatial, Bucket4j(Rate Limit), SpringDoc OpenAPI |

---

## 🔧 설치 및 실행

### 요구 사항
- Java 17+
- MySQL 8.0+
- Redis (선택: 로컬 기본값 `localhost:6379`)

### 환경 변수

`.env` 또는 `application.yml`에 다음을 설정합니다.

```properties
# DB
MYSQL_ENDPOINT=localhost
MYSQL_USERNAME=stargazer
MYSQL_PASSWORD=your_password

# Redis (선택)
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_KEY=your_jwt_secret_key

# 외부 API (선택)
# MET_NORWAY_USER_AGENT: 비우면 예보 폴백을 쓰지 않는다
# KAKAO_REST_API_KEY: 비우면 지명 검색에 Photon만 쓴다

# OAuth2 (웹 소셜 로그인용, 앱 API에는 미사용)
# SERVER_BASE_URL, CLIENT_REDIRECT_BASE_URL
# GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
# KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET
# NAVER_CLIENT_ID, NAVER_CLIENT_SECRET
```

### 실행

```bash
./gradlew bootRun
```

기본 포트: **8080**

---

## 📡 API 및 문서

- **Base URL (로컬):** `http://localhost:8080`
- **Base URL (운영):** `https://api.byeolbolil.xyz` (배포 환경에 따라 변경 가능)
- **API Prefix:** `/api/v1`

| 문서 | 설명 |
|------|------|
| [Swagger UI](https://api.byeolbolil.xyz/swagger-ui/index.html) | API 명세 및 Try-it-out |
| [OpenAPI JSON](http://localhost:8080/v3/api-docs) | 스펙 다운로드·클라이언트 코드 생성용 |
| [docs/API.md](docs/API.md) | API 기준 문서 (공통 응답, 인증, 오류, 엔드포인트별 필드) |
| [docs/ARCHITECTURE_GUIDE.md](docs/ARCHITECTURE_GUIDE.md) | 4계층 아키텍처·코딩 가이드 |
| [docs/SECURITY_AUTH_GUIDE.md](docs/SECURITY_AUTH_GUIDE.md) | 인증·JWT·쿠키·RTR 정책 |
| [docs/SWAGGER_GUIDE.md](docs/SWAGGER_GUIDE.md) | Swagger/OpenAPI 사용법 |
| [docs/MOBILE_DEVELOPER_GUIDE.md](docs/MOBILE_DEVELOPER_GUIDE.md) | 네이티브 앱에서 챙길 것 |
| [docs/MONITORING_AND_ALERTS.md](docs/MONITORING_AND_ALERTS.md) | 모니터링·알림·ASG·부하 테스트 |

---

## 📁 프로젝트 구조 (요약)

```
src/main/java/xyz/ncookie/stargazer/
├── domain/                    # 도메인별 패키지
│   ├── recommend/              # 밤 추천
│   ├── weather/                # 시간별 예보, 구름 격자
│   ├── skylight/               # 하늘 밝기 격자
│   ├── geocode/                # 지명 검색
│   ├── spot/                   # 관측지 조회
│   ├── member/                 # 회원·인증
│   └── bookmark/               # 북마크
├── global/                     # 공통 설정·예외·보안
│   ├── security/               # JWT, OAuth2, Cookie
│   ├── exception/
│   └── advice/
infra/                          # Terraform 등 인프라
docs/                           # 설계·가이드 문서
```

---

## 🖼️ 스크린샷

![스크린샷 1](assets/images/39.119.82.172_4000_%20%282%29.png)
![스크린샷 2](assets/images/39.119.82.172_4000_%20%281%29.png)

---

## 📌 관련 링크

- **Notion 기획서:** [링크]
- **API 명세:** [docs/API.md](docs/API.md)와 Swagger UI 참고
