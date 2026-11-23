# 점수와 추천 기준값의 근거

별볼일이 관측 조건을 판단할 때 쓰는 숫자와 그 출처를 모아 둔 문서다.
코드에서 이 값을 쓰는 곳에는 같은 출처를 주석으로 단다.

출처의 신뢰도는 네 단계로 적는다.

| 표기 | 뜻 |
|---|---|
| 공식 | 정부기관이나 표준기관 문서 |
| 논문 | 동료 심사를 거친 논문, 또는 논문에 근거한 계산식 |
| 서비스 | 기존 천문 예보 서비스가 공개한 기준 |
| 정책 | 근거가 되는 외부 자료가 없어서 별볼일이 정한 값 |

출처를 찾지 못한 값은 정책으로 표시하고, 왜 그 값을 골랐는지 함께 적는다.

## 상태

2026-11-24 기준 초안이다. 아래 값은 조사 결과로 제안한 것이고 아직 코드에 반영하지 않았다.
값을 확정하면 각 항목의 상태를 바꾸고, 기존 값과 달라지는 부분은 `docs/decisions.md`에도 남긴다.

현재 엔진과 크게 다른 항목은 세 가지다.

| 항목 | 현재 코드 | 제안 |
|---|---|---|
| 밤으로 보는 태양 고도 | -6° 이하 | -12° 이하. -18° 이하에서 감점 없음 |
| 0점 처리하는 구름량 | 70% 이상 | 30% 초과는 관측 가능 시간에서 제외 |
| 달 감점 | 빛나는 면적 비율 × 30 | 위상각으로 계산한 보름 대비 밝기 사용 |

현재 코드의 상수 위치와 충돌 내용은 [issues/scoring.md](issues/scoring.md)에 정리돼 있다.

## 어둠

| 값 | 근거 | 신뢰도 | 상태 |
|---|---|---|---|
| 시민박명 -6°, 항해박명 -12°, 천문박명 -18° (태양 중심의 기하학적 고도) | U.S. Naval Observatory, [Rise, Set, and Twilight Definitions](https://aa.usno.navy.mil/faq/RST_defs) | 공식 | 정의로 채택 |
| -12°보다 낮으면 밝은 별과 별자리를 보기에 충분함 | NOAA NWS Sioux Falls, [Twilight](https://www.weather.gov/fsd/twilight). 천문박명 구간에서 대부분의 사람이 하늘을 어둡다고 느끼고 별과 행성 같은 점광원은 관측할 수 있다고 설명한다 | 공식 | 제안 |
| 은하수, 성운 같은 퍼진 대상은 -18° 이하 필요 | 같은 NWS 문서. 퍼진 천체는 완전히 어두운 하늘이 필요하다고 적혀 있다 | 공식 | 제안 |

"입문자에게는 -12°면 충분하다"는 문장을 그대로 쓴 단일 출처는 없다. NWS 설명을 근거로 별볼일의 정책으로 적용한다.

## 구름

| 값 | 근거 | 신뢰도 | 상태 |
|---|---|---|---|
| 관측 가능한 시간은 구름 30% 이하 | Clear Sky Chart 기후 통계는 투명도 확률을 구름 30% 이하인 시간에서만 센다. [예시 페이지](https://server1.cleardarksky.com/clmt/c/Dallasct.html) | 서비스 | 제안 |
| "구름 거의 없음" 라벨은 20% 미만 | 7Timer! CIVIL 예보는 20% 미만을 clear로 분류한다. [문서](https://www.7timer.info/doc.php?lang=en) | 서비스 | 제안 |
| 구름 예보는 멀리 볼수록 부정확하고, 층별 구름은 더 부정확함 | Ye & Chen, "Ultimate Meteorological Question from Observational Astronomers: How Good is the Cloud Cover Forecast?", MNRAS 428, 3288 (2013), [arXiv:1104.1700](https://arxiv.org/abs/1104.1700). GFS 총운량 오차가 30% 안에 들 확률이 예보 180시간에 걸쳐 73%에서 58%로 떨어진다 | 논문 | 48시간 이후 예보에 신뢰도 낮음 표시를 제안 |
| 상층운(권운)은 넓게 덮어도 얇을 수 있음 | Environment and Climate Change Canada, [Cloud forecast](https://www.canada.ca/en/environment-climate-change/services/weather-general-tools-resources/astronomy/cloud-forecast.html) | 공식 | 상층운 가중치를 하층운보다 낮게 둘 근거. 계수는 출처 없음 |

Clear Outside의 신호등 기준은 공개된 정의가 없어서 인용하지 않는다.

## 달

| 값 | 근거 | 신뢰도 | 상태 |
|---|---|---|---|
| 위상각 α에 따른 달 밝기: I = 10^(-0.4(3.84 + 0.026\|α\| + 4×10⁻⁹α⁴)) | Krisciunas & Schaefer, "A Model of the Brightness of Moonlight", PASP 103, 1033 (1991), [doi:10.1086/132921](https://doi.org/10.1086/132921). 원 논문 스캔은 텍스트로 확인하지 못했고 Yao et al., [arXiv:1304.7107](https://arxiv.org/abs/1304.7107)의 재수록본으로 확인했다. 구현 전에 원 논문과 대조한다 | 논문 | 제안 |
| 보름 대비 밝기: 빛나는 면적 85%일 때 0.34, 50%(반달)일 때 0.09, 15%일 때 0.012 | 위 식으로 계산한 값 | 논문 | 참고 |
| 도시에서는 달 감점을 줄임 | Puschnig, Wallner & Posch, MNRAS (2020), [arXiv:1912.05673](https://arxiv.org/abs/1912.05673). 광해가 심한 곳(천정 16.5 mag/arcsec² 미만)에서는 달 주기에 따른 하늘 밝기 변화가 거의 사라진다 | 논문 | 달 감점과 광해를 곱으로 묶는 근거. 계수는 미정 |
| 보름 대비 밝기 25% 이상인 달이 추천 시간의 절반 넘게 떠 있으면 등급을 한 단계 내림 | 없음 | 정책 | 목업에 적용 |

"빛나는 면적이 50%를 넘으면 은하수가 안 보인다" 같은 규칙은 사진가 블로그에만 있어서 쓰지 않는다.

## 광해

| 값 | 근거 | 신뢰도 | 상태 |
|---|---|---|---|
| Bortle 등급 1~9와 등급별 이름 | Bortle, "Gauging Light Pollution: The Bortle Dark-Sky Scale", Sky & Telescope, 2001년 2월. 원문은 접근이 막혀 [Wikipedia](https://en.wikipedia.org/wiki/Bortle_scale) 등 2차 자료로 확인했다 | 논문(잡지) | 화면 표시용 라벨로 채택 제안 |
| 등급별 육안 한계등급 (1등급 7.6~8.0 … 9등급 4.0 이하) | 같은 기사. 4등급 값은 2차 자료끼리 다르다(6.1~6.5와 6.3~6.5) | 논문(잡지) | 원문 대조 전까지 사용 보류 |
| Bortle ↔ SQM 대응 | 권위 있는 표가 없다. Bortle은 숫자 없이 눈으로 본 모습으로 등급을 정했다. 후보는 자연 대비 인공 밝기 비율로 정의한 Clear Sky Chart 표다. [표](https://server1.cleardarksky.com/csk/faq/2.html). 페이지 스스로 근사값이라고 밝힌다 | 서비스 | 근사로 표시하고 사용 제안 |
| 인공 밝기 비율 등급 (자연 밝기 174 µcd/m² 기준 1% 미만, 1~8%, 8~50%, 50% 이상 688 µcd/m² 미만, 688 µcd/m² 이상은 은하수가 보이지 않음, 3000 µcd/m² 이상은 눈이 어둠에 적응하지 못함) | Falchi et al., "The new world atlas of artificial night sky brightness", Science Advances 2(6), e1600377 (2016), [PMC4928945](https://pmc.ncbi.nlm.nih.gov/articles/PMC4928945/) | 논문 | 은하수 가시 여부 판정 기준으로 제안 |
| 위성 레이디언스 → Bortle 변환표 | 출처 없음. `P:\stargazer-viirs\extract_korea_bortle.py`의 `radiance_to_bortle()` 값은 근거를 찾지 못했다. 위성 값은 그 픽셀에서 위로 나가는 빛이고 하늘 밝기는 수십 km 밖 불빛의 산란까지 합친 값이라, 픽셀 값을 바로 등급으로 바꾸는 방식 자체가 약하다 | 없음 | 교체 필요 |

내부 계산은 SQM이나 Falchi 비율 같은 연속값으로 하고, Bortle은 사용자에게 보여주는 라벨로만 쓰는 것을 제안한다.

## 눈으로 보이는 별 개수

| 값 | 근거 | 신뢰도 | 상태 |
|---|---|---|---|
| 육안 한계등급 = 7.93 - 5·log₁₀(10^(4.316 - SQM/5) + 1) | [Unihedron 계산식](https://www.unihedron.com/projects/darksky/NELM2BCalc.html). Schaefer (1990), PASP 102, 212에 근거한다. lightpollutionmap.info도 같은 식을 쓴다 | 논문 | 제안 |
| 등급별 전천 별 개수: 2.0등 50, 3.0등 174, 4.0등 518, 4.5등 904, 5.0등 1,630, 5.5등 2,887, 6.0등 5,080, 6.5등 8,404 | Yale Bright Star Catalogue 5판 (Hoffleit & Warren 1991)을 [Harvard 미러](http://tdc-www.harvard.edu/catalogs/bsc5.html)에서 받아 V등급 누적 개수를 직접 셌다 | 논문(카탈로그) | 제안 |
| 한 번에 보이는 비율 0.41 | 고도 10° 이상 하늘의 면적 비율 (1 - sin 10°)/2. 별이 고르게 분포하고 지평선 근처 소광이 없다고 가정한 값이다 | 정책(기하학 계산) | 화면에 "약"과 "추정"을 붙여 사용 |

Crumey, "Human contrast threshold and astronomical visibility", MNRAS 442, 2600 (2014), [arXiv:1405.4209](https://arxiv.org/abs/1405.4209)에 따르면 가장 어두운 하늘에서도 보통 시력의 한계등급은 6등 안팎이다. Bortle 기사의 7.6~8.0은 낙관적인 값이라 별 개수 계산에는 쓰지 않는다.

## 습도와 이슬

| 값 | 근거 | 신뢰도 | 상태 |
|---|---|---|---|
| 습도 경고 단계 80%, 90%, 95% | 7Timer! [문서](https://www.7timer.info/doc.php?lang=en) | 서비스 | 80%부터 이슬 주의 표시 제안 |
| 광학계는 복사냉각으로 기온보다 차가워져, 기온이 이슬점에 닿기 전에도 이슬이 맺힘 | Celestron, [What is the dew point and why does it matter for astronomy](https://www.celestron.com/blogs/knowledgebase/what-is-the-dew-point-and-why-does-it-matter-for-astronomy) | 서비스(제조사) | 안내 문구 근거 |
| 투명도는 대기 전체 습도로 예측하고, 지표 습도만으로는 근사 | Environment and Climate Change Canada, [Sky transparency forecast](https://www.canada.ca/en/environment-climate-change/services/weather-general-tools-resources/astronomy/sky-transparency-forecast.html) | 공식 | 지표 습도 기반 감점의 한계로 기록 |

"기온과 이슬점 차이가 몇 도 이하면 이슬"이라는 수치 규칙은 출처를 찾지 못했다.

## 관측 시간대

| 값 | 근거 | 신뢰도 | 상태 |
|---|---|---|---|
| 눈이 어둠에 적응하는 시간: 5~8분에 감도가 크게 바뀌고 40분쯤 평탄해짐 | Kalloniatis & Luu, [Light and Dark Adaptation](https://www.webvision.pitt.edu/book/part-viii-psychophysics-of-vision/light-and-dark-adaptation/), Webvision | 논문(교재) | 안내 문구에 사용 |
| 맑은 시간 3시간 이상이면 "꼭 가볼 만해요" | 맑은 시간 길이를 추천 기준으로 쓰는 서비스나 자료를 찾지 못했다. 적응 시간 40분쯤을 빼고도 2시간 넘게 볼 수 있도록 정했다 | 정책 | 사용자 설정으로 조정 가능 |
| 새벽 2시 전에 맑은 시간을 우선 | 입문자가 무리 없이 다녀올 수 있는 시간대로 정했다 | 정책 | 사용자 설정으로 조정 가능 |
| 이번 주 예보 신뢰도: 내일까지 높음, 사흘 뒤까지 보통, 그 뒤 낮음 | 구름 예보 오차가 예보 시점이 멀수록 커진다(Ye & Chen 2013, 위 구름 항목). 구간 경계는 별볼일이 정했다 | 정책 | 이번 주 추천 화면에 표시 |

## 아직 근거가 없는 값

현재 `StargazingScoringEngine`의 감점 계수(광해 `(Bortle-1)*6.25`, 달 `×30`, 습도 `(humidity-60)/2`, 시정 단계별 감점)는 출처를 찾지 못했다.
위 근거로 계산식을 다시 만들 때까지 이 값들은 검증된 것으로 보지 않는다.
