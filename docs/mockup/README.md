# 별볼일 화면 목업

2026-11-24에 방향을 정하면서 만든 클릭 가능한 목업이다. 구현할 때 화면 구성과 동작의 기준으로 쓴다.
`index.html`을 브라우저로 열면 된다. 같은 내용이 https://claude.ai/artifact/UncuWf7Fsm5i46WN7Xe3FJ 에도 올라가 있다.

장소 이름은 실제지만 구름, 이동 시간, 점수는 예시 데이터다. 오른쪽 아래 "목업 도구"에서 시나리오(좋은 밤, 보름달 밤,
전국 흐림), 로그인 여부, 예보 단위(1시간과 3시간)를 바꿀 수 있다.

## 담긴 화면

- 오늘 밤 추천: 가장 좋은 장소 하나, 추천 시간대, 밤 시간 띠(훑으면 시간별 하늘), 짧은 후보 목록, 이번 주 좋은 밤
- 자세히 보기: 관측 점수, 광해 등급, 달 밝기, 습도, 보이는 별 수 추정, 시간별 구름과 점수 그래프
- 지도: 밤 불빛, 구름, 일반 지도 레이어. 확대와 이동, 구름 시간 슬라이더, 가는 길 구름, 이동 중 예보 악화 시 대안 제안
- 설정: 간단히/자세히 보기, 몇 시까지 볼 수 있는지, 얼마나 멀리 갈 수 있는지, 고급 기준
- 추천 기준과 출처: 기준값마다 근거와 상태(출처 있음, 근사, 추정, 정책, 출처 없음)

PC(1080px 이상)는 왼쪽 지도와 오른쪽 추천 패널, 모바일은 한 줄 화면과 하단 탭으로 나뉜다.

## 이미지와 데이터 출처

| 파일 | 출처 | 라이선스 |
|---|---|---|
| `sky-clear.jpg` | ["밤하늘의 은하수"](https://commons.wikimedia.org/wiki/File:%EB%B0%A4%ED%95%98%EB%8A%98%EC%9D%98_%EC%9D%80%ED%95%98%EC%88%98.jpg), 이원웅(INSOLMAE) | CC BY-SA 3.0 |
| `sky-moon.jpg` | ["덕유산의 밤"](https://commons.wikimedia.org/wiki/File:%EB%8D%95%EC%9C%A0%EC%82%B0%EC%9D%98_%EB%B0%A4.jpg), Mg shin | CC BY-SA 3.0 |
| `sky-cloudy.jpg` | ["Night Clouds"](https://commons.wikimedia.org/wiki/File:Night_Clouds_(11424694453).jpg), Bureau of Land Management Oregon and Washington | 퍼블릭 도메인 |
| `korea-night.jpg` | VIIRS Nighttime Lights 2024 (Earth Observation Group, Payne Institute) 기반 `light_pollution_korea.csv`(2026-11-27에 지움, 커밋 `e987548`에 있다)를 `scripts/mockup/render_night_map.py`로 그림 | |
| `korea-base.jpg` | [Natural Earth](https://www.naturalearthdata.com/) 10m 레이어를 `scripts/mockup/render_base_map.py`로 그림 | 퍼블릭 도메인 |

사진은 1400px로 줄이기만 했고 잘라내거나 보정하지 않았다.
