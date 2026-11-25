"""skyglow.py가 논문의 모형을 제대로 옮겼는지, 논문이 실측값을 실은 24곳에서 확인한다.

Duriscoe et al. 2018의 표 1은 미국 남서부 24곳에서 전천 카메라로 잰 ALR이다(2013~2017년). 같은 곳의 ALR을
이 저장소의 계산으로 구해서 나란히 적는다. 위성 자료의 연도가 다르고(여기서는 2024년 연간 합성) 그 사이에
조명이 바뀌었으므로 값이 똑같이 나오지는 않는다. 자릿수와 순서가 맞는지를 본다.

논문의 상수(562.72)를 그대로 쓰면 기울기가 1.253으로 나온다. skyglow.py는 그만큼 상수를 키워 쓰므로 지금
이 스크립트를 돌리면 기울기가 1에 가깝게 나와야 한다. 위성 자료를 다른 해의 것으로 바꾸면 여기서 기울기를
다시 구해 skyglow.py의 PRODUCT_SCALE을 고친다.

    python validate_model.py <VNL .tif 또는 .tif.gz>
"""
import sys

import numpy as np
import rasterio
from rasterio.windows import from_bounds

from skyglow import PRODUCT_SCALE, all_sky_ratio

# 표 1: (이름, 경도, 위도, 실측 ALR)
SITES = [
    ("San Joaquin Ridge", -119.0639, 37.6800, 0.15),
    ("Ashurst Lake", -111.4309, 35.0271, 0.27),
    ("Mesa Verde NP", -108.3674, 37.2363, 0.31),
    ("Enchanted Rock SP", -98.8240, 30.5028, 0.55),
    ("Rocky Mountain NP", -105.7240, 40.4143, 0.78),
    ("Cottonwood Cove Rd", -114.7902, 35.4713, 1.11),
    ("Marine Corps Training Center", -116.2557, 34.3103, 1.30),
    ("Anza Borrego Desert SP", -116.2892, 33.1636, 1.33),
    ("Temple Bar Airstrip", -114.3353, 36.0043, 1.39),
    ("Echo Bay Airstrip", -114.4693, 36.3201, 1.69),
    ("Johnson Valley", -116.6659, 34.3859, 1.77),
    ("Puerta La Cruz Rd", -116.6451, 33.3402, 2.31),
    ("Lucerne Valley", -117.0683, 34.4444, 3.96),
    ("Palomar Observatory", -116.8602, 33.3487, 4.03),
    ("El Mirage Dry Lake", -117.5579, 34.6353, 6.00),
    ("Ramona", -116.8792, 33.1235, 6.37),
    ("Tule Springs NM", -115.4154, 36.4259, 7.56),
    ("Calville Bay Turnoff", -114.7528, 36.1836, 9.79),
    ("Lake Elsinore", -117.3632, 33.6216, 14.07),
    ("Lakeview", -117.1080, 33.8285, 14.41),
    ("Government Wash", -114.8338, 36.1517, 15.96),
    ("Canyon Lake", -117.2692, 33.7130, 17.81),
    ("San Gabriel NM", -117.7591, 34.2180, 22.60),
    ("Mt. Charleston Rd / Hwy 95 Jct.", -115.3069, 36.3277, 27.84),
]

# 24곳과 그 둘레 300km가 드는 범위 (서, 남, 동, 북)
BOUNDS = (-123.0, 27.6, -95.2, 43.3)


def main(source: str) -> None:
    path = f"/vsigzip/{source}" if source.endswith(".gz") else source
    with rasterio.open(path) as src:
        window = from_bounds(*BOUNDS, src.transform).round_offsets().round_lengths()
        radiance = src.read(1, window=window)
        transform = src.window_transform(window)
    ratio = all_sky_ratio(radiance, transform)

    observed, predicted = [], []
    print(f"{'관측지':34s} {'실측':>7s} {'계산':>7s} {'계산/실측':>9s}")
    for name, lon, lat, alr in SITES:
        col, row = ~transform * (lon, lat)
        value = float(ratio[int(row), int(col)])
        observed.append(alr)
        predicted.append(value)
        print(f"{name:34s} {alr:7.2f} {value:7.2f} {value / alr:9.2f}")
    observed, predicted = np.array(observed), np.array(predicted)
    slope = float((observed * predicted).sum() / (observed * observed).sum())
    r = float(np.corrcoef(observed, predicted)[0, 1])
    log_r = float(np.corrcoef(np.log10(observed), np.log10(predicted))[0, 1])
    print(f"원점을 지나는 회귀의 기울기 {slope:.2f}, 상관계수 {r:.3f}, 로그값 상관계수 {log_r:.3f}, "
          f"계산/실측의 중앙값 {np.median(predicted / observed):.2f}")
    print(f"논문의 상수를 그대로 썼을 때의 기울기 {slope * PRODUCT_SCALE:.3f}")


if __name__ == "__main__":
    main(sys.argv[1])
