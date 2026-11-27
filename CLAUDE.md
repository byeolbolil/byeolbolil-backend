# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

The product is called 별볼일 (byeolbolil). "Stargazer" and the `xyz.ncookie.stargazer` package are the old working
name. As of 2026-11-24 the product is being rebuilt as a beginner-first "best spot and time window tonight"
recommender. See `docs/decisions.md` (2026-11-24 entries), `docs/ROADMAP.md`, and the clickable mockup in
`docs/mockup/`.

Stargazer (`xyz.ncookie.stargazer`) is a Spring Boot 4 / Java 17 REST API backend that tells, for each place, whether
and when tonight (or any of the next seven nights) is good for stargazing. It combines an hourly forecast
(Open-Meteo, MET Norway as fallback), sky brightness computed from VIIRS night lights, and sun/moon positions (via
`commons-suncalc`). It also supports member auth, observation spots, bookmarks, place-name search, and a cloud map.
The original single-moment 0–100 score API (OpenWeatherMap, Bortle table, Gemini comment) was removed on 2026-11-27.

## Commands

```bash
# Run the app locally (default port 8080)
./gradlew bootRun

# Build (skip tests, matches CI)
./gradlew clean build -x test

# Run all tests (Docker must be running: integration tests start MySQL and Redis via Testcontainers)
./gradlew test

# Run a single test class
./gradlew test --tests "xyz.ncookie.stargazer.StargazerApplicationTests"

# Regenerate openapi.json from a running local instance (localhost:8080)
./gradlew generateOpenApi
```

Local infra dependencies (MySQL 8 + Redis) are started via:

```bash
docker-compose -f docker-compose.dev.yml up -d
```

Required env vars (see `.env`, loaded via `application.yml`): `MYSQL_ENDPOINT/USERNAME/PASSWORD`, `REDIS_HOST/PORT`,
`JWT_KEY`, and OAuth2 client id/secret triples for Google/Kakao/Naver
(`SERVER_BASE_URL`, `CLIENT_REDIRECT_BASE_URL`) — only needed for the web OAuth2 login flow, not the mobile app API.
Optional: `MET_NORWAY_USER_AGENT` (the fallback forecast source is skipped while it is empty), `OPEN_METEO_BASE_URL`,
`OPEN_METEO_MODEL`, `PHOTON_BASE_URL`, `KAKAO_REST_API_KEY` (place search uses Photon alone while it is empty; the
key also has a caller-IP allowlist in the Kakao console, so a new server IP must be added there).

### Tests

Tests need neither `.env` nor the dev containers; `src/test/resources/application-test.yml` supplies fake values.

- Integration tests extend `support/IntegrationTestSupport` (full context, Testcontainers MySQL + Redis, `MockMvcTester`).
  Keep one shared context: a test with different context configuration starts a second set of containers.
- External APIs are never called. `support/ExternalApiStubs` binds a `MockRestServiceServer` to the app's `RestTemplate`
  and serves JSON from `src/test/resources/fixtures/`; an unstubbed call fails the test.
- Light pollution needs no setup: the night recommendation tests use the real sky brightness grid on the classpath
  and the `lights_in_view` values from `data.sql`.
- Redis cache writes are asynchronous (Spring Data Redis 4 default), so a test that reads Redis directly must wait for
  the key instead of asserting right after the call.
- The engine, grade-table, and API tests are characterization tests: they pin what the code does today, not what is
  correct. When a scoring value changes on purpose, update the expectation in the same change and say so.

## Architecture

### Layering (strictly enforced — see `docs/ARCHITECTURE_GUIDE.md`)

Each `domain/{name}/` package follows a 4-layer structure with **unidirectional** dependencies:

```
Controller → Application → Domain → Repository
```

- **Controller**: HTTP binding/validation only, no business logic. Builds a `*Command` via `Command.from(...)` and
  delegates to an Application Service.
- **Application** (`{Domain}ApplicationService`): use-case orchestration, `@Transactional` boundary. This is the
  **only** layer allowed to call another domain's `{Domain}DomainService`. Simple reads/writes with no domain rules
  may call the Repository directly instead of going through the Domain Service.
- **Domain** (`{Domain}DomainService`): single-domain business rules, entity state transitions, validation
  (e.g. `findById`, `validateOwner`). Must never reference another domain's Domain Service or call up into
  Application.
- **Repository**: plain Spring Data JPA, persistence only, no business logic.

Cross-domain communication always goes through Application-layer orchestration — never Domain → Domain.

### Domains (`src/main/java/xyz/ncookie/stargazer/domain/`)

- `weather/` — hourly forecast in an internal model (`WeatherForecast`, `HourlyWeather`).
  `WeatherDomainService.getHourlyForecast(lat, lon)` returns up to 168 hours from the current hour: Open-Meteo
  (`ecmwf_ifs`) first, MET Norway as fallback, cached in Redis by 0.05° cell (`ForecastCell`) and model run time
  (`ForecastCache`). Failures surface as `WeatherErrorCode.FORECAST_UNAVAILABLE`, never as dummy weather.
  `getCloudGrid()` fetches total cloud cover for a 17×18 grid over Korea (`CloudGridSpec.KOREA`, 0.4°) in one
  Open-Meteo call per model run; `GET /api/v1/clouds/nights/{date}` serves one night of it for the map's cloud layer.
- `geocode/` — place-name search and reverse lookup for the map search box and the travel origin.
  `GeocodeDomainService` asks two sources and merges them: the public Photon server (OpenStreetMap data, no key),
  which knows cities, counties and mountains, and Kakao Local (`client/kakao`, keyword and address search), which
  knows viewpoints, parking lots and lot-number addresses. Photon results are cached in Redis for 24 hours
  (`GeocodeCache`); Kakao results are never cached because Kakao does not allow storing them. Outbound calls are
  capped per source (`PhotonRateLimitConfig`, `KakaoRateLimitConfig`), and one source failing does not fail the
  search. Reverse lookup uses Photon only. Serves `GET /api/v1/geocode/search` and `/reverse` (public).
- `skylight/` — sky brightness and nearby light by coordinate. `SkyBrightnessGrid` loads
  `src/main/resources/skyglow/skyglow_korea.bin.gz` (built by `scripts/lightpollution/`) into memory at startup; no
  DB table. The frontend's light-pollution map layer is drawn from the same file. `KoreaSkyRank` reads
  `skyglow/korea_rank.csv` (built by `korea_rank.py`) and turns a sky brightness into a 1–100 rank among South Korean
  land, sent as `skyRank` because the five sky classes (US national-park scale) put almost all of Korea in two classes.
- `spot/` — observation spot lookup by lat/lng + radius, backed by MySQL spatial indexing (Hibernate Spatial).
  Spots are either `OPEN_SITE` (free night access, the only recommendation candidates) or `FACILITY`
  (observatories and science museums, served separately via `?kind=FACILITY`). `src/main/resources/data.sql` is the
  source of truth: it upserts by explicit id on every start and marks removed spots `RETIRED` instead of deleting
  them. Coordinates point at the named `viewpoint` (a parking lot or lookout), not at the area in general; where that
  spot could not be pinned on a map the `guide` says the position is approximate. Reference links per spot live in
  `observation_spot_link`, which `data.sql` empties and refills on every start. Named viewing points inside one spot
  (a mountain with a parking lot and a meadow twenty minutes away on foot) live the same way in
  `observation_spot_point`; the first point is where the car is parked. YouTube videos shot at a spot live in
  `observation_spot_video`: `scripts/spots/videos.tsv` is the curated list and `scripts/spots/videos.py` turns it
  into the `data.sql` rows by asking YouTube's oEmbed endpoint for each title and channel. Spots whose road closes in winter
  carry this season's `closed_from`/`closed_until` in `data.sql`; they are left out of the recommendation candidates
  for nights in that period (bookmarked places are still evaluated). Update the dates every season.
- `member/` — auth/account (OAuth2 + JWT), profile.
- `bookmark/` — CRUD for SPOT-type or CUSTOM-type saved locations.
- `recommend/` — tonight's recommendation. `engine/NightAssessmentEngine` evaluates one night per place from the
  hourly forecast (dark hours, clear windows, night pattern, level); `NightRecommendationApplicationService` gathers
  default spots plus the member's bookmarks, ranks them, and serves `GET /api/v1/recommendations/tonight` and
  `/nights/{date}` (public; bookmarks only when logged in). The same service answers "when is this place good":
  `/week?lat&lon` evaluates seven nights for any coordinate and `/week/mine` does it for every bookmark, each from
  one forecast per place and without travel time. `/sky?lat&lon` returns only the sky brightness of a point (no
  forecast) for the map's tap-anywhere card. `/moon?lat&lon` returns six weeks of moon phase, rise/set and moonless dark
  hours for a point (no forecast, `engine/MoonCalendar`) for the header's moon calendar sheet. Thresholds cite `docs/SCORING.md` at the point of use.
  Light pollution enters as a `LightCondition` of three values, none of them a Bortle class: sky brightness (ALR, the
  all-sky artificial brightness as a multiple of the natural sky, Duriscoe et al. 2018) and the mean radiance within
  1 km, both looked up by coordinate from `skylight/provider/SkyBrightnessGrid` (through `SkyLightDomainService`)
  (`src/main/resources/skyglow/skyglow_korea.bin.gz`), and `lights_in_view`, a per-spot index of distant lights not
  hidden by terrain, stored in `data.sql`. `scripts/lightpollution/` computes all of them from VIIRS night lights and
  the Copernicus DEM; the raw data lives outside the repo. Re-run `terrain.py` when a spot's coordinates change.

### Global layer (`global/`)

- `security/` — JWT (access token via `Authorization` header) + OAuth2 (Google/Kakao/Naver) + Refresh Token Rotation
  stored in Redis and set as an HttpOnly cookie. See `docs/SECURITY_AUTH_GUIDE.md` for the full flow, error codes,
  and client integration requirements (mobile clients must configure a cookie jar or RT will be lost). Auth services
  are decoupled from HTTP Request/Response objects.
- `exception/` — `BaseException` + per-domain `{Domain}ErrorCode` enums implementing `ErrorCode`
  (status + message), handled centrally by `GlobalExceptionHandler`.
- `advice/ResponseBodyWrapper` — a `ResponseBodyAdvice` that wraps every `@RestController` JSON response (except
  `/v3/api-docs` and `/swagger-ui`) in a common `CommonResponse` envelope (`success`, `status`, `message`, `data`);
  skip wrapping by returning a `CommonResponse` directly. Success messages come from the `@ResponseMessage`
  annotation on the controller method, defaulting to a generic Korean success message if absent.
- `config/` — Bucket4j rate limiting for Photon and Kakao Local (`PhotonRateLimitConfig`, `KakaoRateLimitConfig`),
  RestTemplate, Swagger/OpenAPI setup. There is no Spring cache abstraction; forecast and geocode results are cached
  by writing to Redis directly (`ForecastCache`, `GeocodeCache`).

### API conventions

- Base path `/api/v1`, versioned OpenAPI spec exported to `openapi.json` via `./gradlew generateOpenApi`.
- All responses go through the `CommonResponse` envelope described above.
- DTO naming: `{Action}{Domain}Request`/`Response` in `dto/request`, `dto/response`; Application-layer input is a
  `{Action}{Domain}Command` record with a static `from(...)` factory converting from the Request DTO.
- `docs/API.md` is the client-facing reference (envelope, auth flow, error shapes, per-endpoint fields and when they
  are absent). Change it in the same commit as any API change. The `*ApiTest` integration tests pin the responses it
  quotes. `openapi.json` is gitignored on purpose; the live spec at `/v3/api-docs` is the machine-readable source.
- Error statuses: 401 only when the token or the login credentials are wrong (clients answer 401 with a token
  reissue), 403 for someone else's resource, 409 for duplicates, 400 for unreadable or invalid input.

### Infrastructure (`infra/`)

The backend runs on one Oracle Cloud VM (1 GB, `VM.Standard.E2.1.Micro`) as a docker compose stack: Caddy
(HTTPS for `api.byeolbolil.xyz`), the app, MySQL, and Redis. `infra/oci/deploy/` holds the compose file and Caddyfile.
`.github/workflows/deploy.yml` runs on every push to `dev` that touches more than docs (`docs/**`, `*.md`,
`scripts/issues/**` alone do not deploy): build with tests, push the image to GHCR, copy the
compose files and a `.env` rendered from GitHub secrets to `/opt/stargazer` on the VM, then `docker compose up -d`.
The server's `.env` is overwritten on each deploy, so change values in GitHub secrets or variables, never on the VM.
The frontend is a separate repository deployed by Vercel at `www.byeolbolil.xyz`. Vercel builds `master` as production
and every other branch (including `dev`) as a preview, so releasing the frontend means fast-forwarding `master` to `dev`.

The Terraform files directly under `infra/` are from the AWS setup, which no longer exists (free tier ended).
The OCI Terraform lives only on the unmerged `feature/oci-migration` branch and describes an earlier design with a
managed MySQL. `*.pem`, `*.tfstate*`, and `*.tfvars` under `infra/` are gitignored — never commit these even if
they appear as untracked local files.

## Conventions

- All chat responses and git commit messages should be written in Korean, using Conventional Commits
  (`feat:`, `fix:`, `refactor:`, `docs:`, etc. — see `.cursorrules`).
- **Claude owns branches and commits (since 2026-11-24).** The user now focuses on product direction and real-world
  use, and has handed over git work and testing. This replaces the earlier rule that the user commits everything.
  - Branch off `dev` for each unit of work (`feat/…`, `fix/…`, `refactor/…`, `test/…`, `docs/…`, `chore/…`) and
    merge back into `dev` with `--no-ff` when the unit is done.
  - Split commits by logical change. Write Korean Conventional Commits in the existing style: a subject line, then
    a bullet body that says why. Add no AI co-author trailer, since the history must not read as machine-generated.
  - After committing, tell the user in a few Korean lines what each commit changed.
  - GitHub issues are Claude's too (since 2026-11-24): comment, edit bodies and checklists, close, and create
    issues without asking, following the issue conventions below.
  - `git push` needs no go-ahead since 2026-11-27: push `dev` when a unit of work is merged (a backend push to `dev`
    deploys, see Infrastructure). `gh pr create` and other ways of publishing code still need the user's go-ahead.
  - Never commit secrets or the gitignored `infra/` files described above.
- **Tests come with every feature.** Write unit tests for domain logic, including boundary values, and
  `@SpringBootTest` integration tests for API paths, using Testcontainers for MySQL and Redis. Stub external APIs
  (weather, geocoding) with recorded responses, and never call them from tests. Make `./gradlew test` pass
  before every commit that touches code. The build order and done criteria live in `docs/ROADMAP.md`.
- **Frontend changes run use-case scenarios too (since 2026-11-27).** Before committing a screen change in the
  frontend repo, pass `pnpm test`, `pnpm typecheck`, `pnpm walkthrough` and `pnpm scenarios` against a running dev
  server (see the frontend README). `scripts/scenarios.mjs` simulates real use across screens (sheets and the back
  button, the place view, list and map agreeing on the night, the mobile map tab, long press, the cloud time bar).
  Add a scenario for every new interaction and for every cross-screen bug, and confirm the bug's scenario fails on
  the code before the fix. This rule exists because a map showing a different night than the place view shipped,
  and a walkthrough step broke unnoticed, when only unit tests and spot screenshots were run.
- **Decisions and trade-offs have to end up somewhere the repo can see.** Whenever work involves a real choice —
  a policy, a threshold, an approach with a discarded alternative — say so and point at the right home instead of
  leaving the reasoning in chat or in a GitHub issue that will be closed. A cloned repo must still explain itself.
  - scoring numbers and their sources → `docs/SCORING.md`, plus the citation comment at the point of use
    (see the domain-value rule below)
  - everything else — policy, architecture, performance, deployment → `docs/decisions.md`, one dated entry per
    decision: what was chosen, why, what was rejected, and what it affects. Call out client-facing impact
    explicitly so `docs/MOBILE_DEVELOPER_GUIDE.md` can be updated.
  - the deliberation itself (options weighed, things tried, dead ends) belongs in the GitHub issue comments;
    append the conclusion to the issue body so it isn't buried under comments
- **Codebase investigations are organised by area.** Each area pairs a write-up at `docs/issues/{area}.md`
  (constant inventory, conflict checks, findings with `file:line` evidence pinned to permalinks) with a one-shot
  `gh issue create` script at `scripts/issues/{area}.sh`. Index and conventions live in `docs/issues/README.md`.
  Investigation docs are point-in-time snapshots and are not retro-edited as work lands.
  Priority (P0–P4) and Size (XS–XL) are **org-level Issue fields, never labels or title prefixes** — duplicating
  them guarantees drift. Only `area:*` is a repo label. `gh issue create` has no flag for these fields, but the
  script sets them via the GraphQL `setIssueFieldValue` mutation, resolving field and option IDs by name
  (never hardcode the IDs — they change if a field is recreated). Issue dependencies use `--blocked-by` with
  captured issue numbers, not free text. Areas: `scoring` (investigated and registered);
  `architecture`, `security`, `infra` planned. `scripts/issues/scoring.sh` and `scripts/issues/scoring/*.md`
  are the reference implementation — match their shape when adding an area. Always run
  `gh issue list --state all` before writing a new area's script, and fold findings that overlap an
  already-registered issue into that issue instead of creating a duplicate one.
- **This is a portfolio project — nothing should read as machine-generated.** That applies to issue bodies, docs,
  and commit messages alike. Concretely, from feedback already given:
  - Issue titles are a short noun phrase with no `[P1]` prefix and no `— subtitle` tail
    ("광해 데이터 파이프라인 재작업", not "[P1] 광해 데이터 파이프라인 재작업 — 등급 변환을 코드로 이관").
  - Avoid the tells: em dashes used as connectors mid-sentence, bold scattered through paragraphs, `⚠`-style
    marker glyphs, and headings that presuppose an AI/human split (write "결정 필요", never "사람이 결정해야 할 것").
  - Korean particles attach directly to code spans: `` `radiance`를 ``, not `` `radiance` 를 ``.
  - Checklist items end in a verb so they read as work, not as an inventory.
  - Bare URLs on their own line do **not** render as GitHub code previews inside list items — always use
    `[`File.java#L10-L20`](url)` markdown links.
- **Domain-science values require a citation.** Any numeric threshold, coefficient, or classification table derived
  from astronomy/photometry domain knowledge (e.g. sky brightness class boundaries, the skyglow model's constants,
  scoring weights, twilight altitude cutoffs) must not be added or changed without a traceable source:
  - Find a concrete reference first (paper, official standard, dataset/tool documentation) that justifies the number
    — don't invent a plausible-looking value.
  - Add a code comment at the point of use citing that reference (author/title/year or URL), not just a description
    of what the number does.
  - If no reference can be found, say so explicitly instead of guessing — an honest "unknown" is better than a
    fabricated citation.
  - This rule exists because the original engine's coefficients (a `radiance_to_bortle` table and the
    `(Bortle-1)*6.25` / moon `×30` weights) had no recoverable source. They were dropped in favour of sourced values
    and the engine was removed on 2026-11-27; `docs/SCORING.md` marks which remaining weights are policy, not
    literature.
- Further docs: [docs/API.md](docs/API.md) (client-facing API reference),
  [docs/ARCHITECTURE_GUIDE.md](docs/ARCHITECTURE_GUIDE.md) (layering rules, full checklist),
  [docs/SECURITY_AUTH_GUIDE.md](docs/SECURITY_AUTH_GUIDE.md) (auth flow),
  [docs/MOBILE_DEVELOPER_GUIDE.md](docs/MOBILE_DEVELOPER_GUIDE.md), [docs/SWAGGER_GUIDE.md](docs/SWAGGER_GUIDE.md),
  [docs/MONITORING_AND_ALERTS.md](docs/MONITORING_AND_ALERTS.md), [docs/SCORING.md](docs/SCORING.md) (threshold
  sources), [docs/ROADMAP.md](docs/ROADMAP.md) (build order), [docs/mockup/](docs/mockup/) (target screens).
