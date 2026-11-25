# 광해 자료 만들기

밤 추천이 쓰는 불빛 자료를 위성 자료와 고도 자료에서 계산하는 스크립트다. 결과만 저장소에 들어 있고 원본 자료는
크기 때문에 저장소 밖에 둔다. 값의 근거와 한계는 [docs/SCORING.md](../../docs/SCORING.md)의 "광해" 절, 이렇게 정한 이유는
[docs/decisions.md](../../docs/decisions.md)의 2026-11-26 항목에 있다.

## 만드는 것

| 결과 | 어디에 들어가나 | 만드는 스크립트 |
|---|---|---|
| 하늘 밝기와 둘레 1km 불빛의 격자 | `src/main/resources/skyglow/skyglow_korea.bin.gz` | `crop_radiance.py`, `skyglow.py`, `export_grid.py` |
| 명소마다 지형 너머로 직접 보이는 불빛의 지수 | `src/main/resources/data.sql`의 `lights_in_view` | `terrain.py` |

## 원본 자료

| 자료 | 받는 곳 | 크기 | 이용 조건 |
|---|---|---|---|
| VIIRS Nighttime Lights V2 연간 합성 2024, `VNL_npp_2024_global_vcmslcfg_v2_c202502261200.average_masked.dat.tif.gz` | [Earth Observation Group](https://eogdata.mines.edu/products/vnl/) (계정이 필요하다) | 330MB, 풀면 11GB | CC BY 4.0. Earth Observation Group, Payne Institute for Public Policy를 출처로 적고 Elvidge et al. 2021을 인용한다 |
| Copernicus DEM GLO-30, 북위 32~39도 동경 124~130도의 1도 타일 40장 | `https://copernicus-dem-30m.s3.amazonaws.com/Copernicus_DSM_COG_10_N37_00_E128_00_DEM/Copernicus_DSM_COG_10_N37_00_E128_00_DEM.tif` 꼴의 주소 (계정 없이 받는다) | 850MB | 출처를 적으면 자유롭게 쓸 수 있다. © DLR e.V. 2010-2014 and © Airbus Defence and Space GmbH 2014-2018 provided under COPERNICUS by the European Union and ESA; all rights reserved |

비교에만 쓴 자료가 하나 더 있다. Falchi et al. 2016의 세계 지도([doi:10.5880/GFZ.1.4.2016.001](https://doi.org/10.5880/GFZ.1.4.2016.001), 650MB)는
재배포와 상업적 이용이 금지돼 있어서 결과물에는 들어가지 않는다.

## 순서

Python 3.10에 `rasterio`, `numpy`, `scipy`가 있으면 된다. 아래에서 `$DATA`는 원본 자료를 둔 폴더다.

```bash
# 1. 위성 합성에서 한국과 둘레 300km를 잘라 낸다 (1분)
python crop_radiance.py $DATA/VNL_npp_2024_global_vcmslcfg_v2_c202502261200.average_masked.dat.tif.gz $DATA/work/vnl2024_korea.tif

# 2. 하늘 밝기(ALR)와 둘레 1km 평균 불빛을 계산한다 (1분)
python skyglow.py $DATA/work/vnl2024_korea.tif $DATA/work/alr_korea.tif

# 3. 앱이 읽는 파일로 바꾼다
python export_grid.py $DATA/work/alr_korea.tif ../../src/main/resources/skyglow/skyglow_korea.bin.gz

# 4. 명소마다 직접 보이는 불빛을 계산한다 (지점 40곳에 3분). 지점 CSV의 열은 id,name,lat,lon
python terrain.py $DATA/dem $DATA/work/vnl2024_korea.tif $DATA/work/points.csv $DATA/work/terrain.csv
```

4번의 결과에서 `direct_light` 열을 `data.sql`의 `lights_in_view`에 옮겨 적는다. 좌표를 특정한 열린 관측지만 적는다.
좌표가 대략의 위치인 곳은 몇십 m에 따라 값이 크게 달라져서 비워 둔다.

격자를 다시 만들면 `SkyBrightnessGridTest`의 한국 격자 테스트가 값의 범위를 확인한다.

## 모형이 맞게 옮겨졌는지 확인하기

```bash
python validate_model.py $DATA/VNL_npp_2024_global_vcmslcfg_v2_c202502261200.average_masked.dat.tif.gz
```

하늘 밝기 모형을 낸 논문(Duriscoe et al. 2018)이 실측값을 실은 미국 남서부 24곳을 같은 코드로 계산해 나란히 적는다.
2026-11-26에 돌린 결과는 상관계수 0.991이고, 논문의 보정 상수를 그대로 쓰면 실측보다 1.253배 크게 나왔다. `skyglow.py`는
그만큼 상수를 키워 쓴다(`PRODUCT_SCALE`). 위성 자료를 다른 해의 것으로 바꾸면 여기서 기울기를 다시 구한다.

## 2026-11-26의 계산값

열린 관측지의 값이다. 하늘 밝기는 자연 밤하늘의 몇 배인지이고, 직접 보이는 불빛은 0.2 아래면 거의 보이지 않는 것이다.
괄호 안은 그 자리에서 60m 떨어진 여덟 곳까지 계산했을 때의 가장 작은 값과 큰 값이다.

| 명소 | 하늘 밝기 | 직접 보이는 불빛 | 고도(m) |
|---|---|---|---|
| 영양 반딧불이생태공원 | 1.49 | 0.03 (0.01~0.05) | 301 |
| 평창 육백마지기 | 2.09 | 0.02 (0.02~0.04) | 1214 |
| 정선 문치재 | 2.12 | 0.02 (0.01~0.14) | 728 |
| 정선 타임캡슐공원 | 2.54 | 1.05 (0.69~1.47) | 824 |
| 고성 미시령 옛길 | 3.13 | 좌표가 대략이라 비움 | |
| 함백산 만항재 | 3.15 | 0.52 (0.34~0.81) | 1265 |
| 강릉 안반데기 | 3.17 | 0.18 (0.13~0.28) | 1073 |
| 태안 운여해변 | 3.20 | 좌표가 대략이라 비움 | |
| 거창 감악산 | 3.22 | 0.74 (0.26~0.98) | 901 |
| 합천 황매산 | 3.26 | 0.31 (0.25~0.33) | 737 |
| 합천 오도산 | 3.70 | 1.77 (0.46~2.03) | 1105 |
| 제천 덕주산성 | 3.99 | 0.16 (0.09~0.19) | 280 |
| 영천 보현산 | 4.06 | 0.45 (0.34~0.58) | 1045 |
| 화천 조경철천문대 | 4.11 | 0.39 (0.23~0.96) | 1035 |
| 의령 한우산 | 4.52 | 0.05 (0.02~0.11) | 775 |
| 가평 화악터널 쌈지공원 | 4.56 | 0.37 (0.14~0.50) | 868 |
| 제주 용눈이오름 | 4.65 | 0.21 (0.04~0.66) | 156 |
| 서귀포 1100고지 | 5.51 | 0.02 (0.00~0.08) | 1097 |
| 제주 새별오름 | 5.88 | 1.09 (0.43~2.13) | 423 |
| 부여 성흥산성 | 5.93 | 2.36 (1.52~3.45) | 216 |
| 양평 벗고개 | 6.47 | 좌표가 대략이라 비움 | |
| 제주 마방목지 | 7.08 | 0.20 (0.09~0.94) | 627 |
| 연천 당포성 | 8.67 | 0.00 (0.00~0.06) | 91 |
| 경주 풍력발전단지 | 9.48 | 1.37 (1.17~6.18) | 534 |

후보지는 아니지만 다녀온 사람의 관찰과 맞춰 본 곳: 남원 정령치휴게소는 하늘 밝기 3.33, 직접 보이는 불빛 0.08
(0.08~0.64), 고도 1170m다. 가림이 없다고 치면 지수가 3.5인데 주차장에서는 능선에 가려 대부분 보이지 않는다.
