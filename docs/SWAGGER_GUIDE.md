# Swagger와 OpenAPI 스펙

API 스펙은 SpringDoc이 컨트롤러와 DTO의 어노테이션에서 자동으로 만든다. 사람이 읽는 설명은 [API.md](./API.md)에 있고,
이 문서는 스펙을 어디서 보고 어떻게 받는지만 다룬다.

## 보는 곳

| | 로컬 | 운영 |
|---|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` | `https://api.byeolbolil.xyz/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` | `https://api.byeolbolil.xyz/v3/api-docs` |

운영 서버는 `dev` 브랜치에 push될 때마다 다시 배포되므로 운영 주소의 스펙이 곧 `dev`의 스펙이다.

Swagger UI에서 로그인이 필요한 API를 불러 보려면 `POST /api/v1/auth/login`으로 받은 `accessToken`을 오른쪽 위
Authorize에 넣는다.

## 파일로 받기

```bash
# 운영 서버에서
curl -o openapi.json https://api.byeolbolil.xyz/v3/api-docs

# 로컬 서버를 띄워 둔 상태에서
./gradlew generateOpenApi
```

`openapi.json`은 `.gitignore`에 들어 있다. 받은 파일은 금방 낡으므로 저장소에 넣지 않고 필요할 때 다시 받는다.

## 스펙에 없는 것

- 공통 응답 봉투(`success`, `status`, `message`, `data`). 스펙의 응답 스키마는 `data` 안쪽만 그린다.
- 오류 응답의 모양과 상태 코드별 의미
- 리프레시 토큰 쿠키와 `Authorization-Refresh` 헤더
- `level`, `pattern` 같은 문자열 필드의 값 목록

모두 [API.md](./API.md)에 있다. 스펙으로 클라이언트 코드를 생성할 때는 두 문서를 함께 본다.

## 스펙을 고칠 때

- 엔드포인트 설명은 컨트롤러의 `@Operation`, 필드 설명은 DTO의 `@Schema`에 쓴다.
- 응답 상태 코드를 바꾸면 `@ApiResponse`도 같이 고친다.
- 제목과 서버 목록은 `global/config/SwaggerConfig`에 있다.
