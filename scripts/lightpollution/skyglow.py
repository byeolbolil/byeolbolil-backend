"""위로 새는 불빛(VIIRS 레이디언스)에서 하늘 전체의 인공 밝기 비율(ALR)을 계산한다.

Duriscoe, Anderson, Luginbuhl & Baugh, "A simplified model of all-sky artificial sky glow derived from VIIRS
Day/Night band data", JQSRT 214, 133-145 (2018), doi:10.1016/j.jqsrt.2018.04.028 의 모형이다.

    ALR = (1 / C) * sum( r_i * d_i ^ -alpha(d_i) )           식 (9)
    alpha(d) = 2.3 * (d / 350) ^ 0.28                         식 (11)

r_i는 격자 칸의 레이디언스(nW/cm2/sr), d_i는 관측자에서 그 칸까지의 거리(km)이고 300km 안의 칸을 모두 더한다.
ALR은 하늘 반구 전체에서 평균한 인공 밝기를 자연 밤하늘의 평균 밝기 250 ucd/m2로 나눈 값이다(식 6).

논문은 450m 정사각 칸의 격자에서 더했다(2.5절, 3.2절). 여기서는 15초각 위경도 격자를 그대로 쓰므로 칸의 넓이가
위도에 따라 달라진다. 칸마다 넓이를 450m 칸의 넓이로 나눈 값을 곱해 같은 합이 되게 맞춘다.

보정 상수 C는 논문에서 562.72다(3.2절). 논문은 2014~2017년의 월간 합성으로 이 값을 맞췄고, 여기서 쓰는 2024년
연간 합성(VNL V2 average_masked)으로 논문의 실측 24곳을 다시 계산하면 실측보다 1.25배 크게 나온다
(validate_model.py, 원점을 지나는 회귀의 기울기 1.253, 상관계수 0.99). 논문이 4.3.3절에서 말한 대로 상수를
다시 맞춰 C = 562.72 x 1.253 을 쓴다. 이 차이에는 자료 종류의 차이와 그 사이에 실제로 늘어난 불빛이 섞여 있고
둘을 나눌 방법은 없다. 그래서 여기서 나온 ALR은 논문의 등급 경계(0.33, 2, 10)와 견줄 때 어림값으로 읽어야 한다.

출력의 둘째 밴드는 그 자리 둘레 1km 안의 평균 레이디언스다. 하늘 밝기와 별개로, 지금 서 있는 곳이 가로등과
건물 사이인지를 가리는 데 쓴다. 반경 1km는 위성 센서의 화소(약 750m)보다 조금 넓게 잡은 값이다.

    python skyglow.py <crop_radiance.py의 출력 .tif> <출력 .tif>
"""
import sys

import numpy as np
import rasterio
from rasterio.transform import from_origin
from scipy.signal import fftconvolve

from grid import OUTPUT_STEP, TARGET_BOUNDS

PAPER_CALIBRATION = 562.72    # Duriscoe et al. 2018, 3.2절
PRODUCT_SCALE = 1.253         # 2024년 연간 합성으로 논문의 실측 24곳을 다시 맞춘 비율 (validate_model.py)
CALIBRATION = PAPER_CALIBRATION * PRODUCT_SCALE
RADIUS_KM = 300.0             # 같은 논문 2.5절
REFERENCE_CELL_KM = 0.45      # 같은 논문 3.2절: 450m 격자
KM_PER_DEGREE = 111.195       # 지구 평균 반지름 6371km인 구에서 위도 1도
BAND_DEGREES = 0.25           # 이 위도 폭마다 커널을 새로 만든다. 폭 안에서 cos(위도)는 0.4%보다 적게 바뀐다
NEARBY_RADIUS_KM = 1.0        # 둘레 평균을 내는 반경


def alpha(distance_km: np.ndarray) -> np.ndarray:
    return 2.3 * (distance_km / 350.0) ** 0.28


def weight(distance_km: np.ndarray) -> np.ndarray:
    """거리 가중치 d^-alpha(d). 거리가 0에 가까워지면 1로 간다."""
    safe = np.maximum(distance_km, 1e-6)
    result = safe ** -alpha(safe)
    return np.where(distance_km > RADIUS_KM, 0.0, result)


def kernel(latitude: float, cell_degrees: float) -> np.ndarray:
    """그 위도의 관측자에게 둘레 칸들이 갖는 가중치."""
    dy = cell_degrees * KM_PER_DEGREE
    dx = dy * np.cos(np.radians(latitude))
    half_rows = int(np.ceil(RADIUS_KM / dy))
    half_cols = int(np.ceil(RADIUS_KM / dx))
    rows = np.arange(-half_rows, half_rows + 1) * dy
    cols = np.arange(-half_cols, half_cols + 1) * dx
    distance = np.hypot(rows[:, None], cols[None, :])
    return weight(distance)


def all_sky_ratio(radiance: np.ndarray, transform) -> np.ndarray:
    """레이디언스 격자와 같은 모양의 ALR 격자. 가장자리 300km는 둘레의 불빛이 빠져 있어 쓰면 안 된다."""
    cell = transform.a
    north = transform.f
    height = radiance.shape[0]
    latitudes = north - (np.arange(height) + 0.5) * cell

    cell_km = cell * KM_PER_DEGREE
    area_ratio = (cell_km * cell_km * np.cos(np.radians(latitudes))) / (REFERENCE_CELL_KM ** 2)
    source = np.clip(radiance, 0, None).astype(np.float64) * area_ratio[:, None]

    result = np.zeros_like(source)
    rows_per_band = max(1, int(round(BAND_DEGREES / cell)))
    for start in range(0, height, rows_per_band):
        stop = min(height, start + rows_per_band)
        k = kernel(float(latitudes[start:stop].mean()), cell)
        half = k.shape[0] // 2
        # 이 띠의 줄에 닿는 원본 줄만 잘라서 합성곱한다
        top = max(0, start - half)
        bottom = min(height, stop + half)
        convolved = fftconvolve(source[top:bottom], k, mode="same")
        result[start:stop] = convolved[start - top:stop - top]
    return np.clip(result, 0, None) / CALIBRATION


def nearby_mean(radiance: np.ndarray, transform) -> np.ndarray:
    """칸마다 둘레 NEARBY_RADIUS_KM 안의 평균 레이디언스(nW/cm2/sr)."""
    cell = transform.a
    height = radiance.shape[0]
    latitudes = transform.f - (np.arange(height) + 0.5) * cell
    source = np.clip(radiance, 0, None).astype(np.float64)
    dy = cell * KM_PER_DEGREE
    result = np.zeros_like(source)
    rows_per_band = max(1, int(round(BAND_DEGREES / cell)))
    for start in range(0, height, rows_per_band):
        stop = min(height, start + rows_per_band)
        dx = dy * np.cos(np.radians(float(latitudes[start:stop].mean())))
        half_rows = int(np.ceil(NEARBY_RADIUS_KM / dy))
        half_cols = int(np.ceil(NEARBY_RADIUS_KM / dx))
        rows = np.arange(-half_rows, half_rows + 1) * dy
        cols = np.arange(-half_cols, half_cols + 1) * dx
        disc = (np.hypot(rows[:, None], cols[None, :]) <= NEARBY_RADIUS_KM).astype(np.float64)
        disc /= disc.sum()
        top = max(0, start - half_rows)
        bottom = min(height, stop + half_rows)
        convolved = fftconvolve(source[top:bottom], disc, mode="same")
        result[start:stop] = convolved[start - top:stop - top]
    return np.clip(result, 0, None)


def resample(values: np.ndarray, transform, bounds, step: float) -> tuple[np.ndarray, object]:
    """칸 중심 좌표에서 쌍선형 보간으로 출력 격자를 만든다."""
    west, south, east, north = bounds
    cols = int(round((east - west) / step))
    rows = int(round((north - south) / step))
    lon = west + (np.arange(cols) + 0.5) * step
    lat = north - (np.arange(rows) + 0.5) * step
    x = (lon - transform.c) / transform.a - 0.5
    y = (transform.f - lat) / transform.a - 0.5
    x0 = np.floor(x).astype(int)
    y0 = np.floor(y).astype(int)
    fx = (x - x0)[None, :]
    fy = (y - y0)[:, None]
    a = values[np.ix_(y0, x0)]
    b = values[np.ix_(y0, x0 + 1)]
    c = values[np.ix_(y0 + 1, x0)]
    d = values[np.ix_(y0 + 1, x0 + 1)]
    out = a * (1 - fx) * (1 - fy) + b * fx * (1 - fy) + c * (1 - fx) * fy + d * fx * fy
    return out.astype(np.float32), from_origin(west, north, step, step)


def main(source: str, target: str) -> None:
    with rasterio.open(source) as src:
        radiance = src.read(1)
        transform = src.transform
        crs = src.crs
    ratio = all_sky_ratio(radiance, transform)
    out, out_transform = resample(ratio, transform, TARGET_BOUNDS, OUTPUT_STEP)
    nearby, _ = resample(nearby_mean(radiance, transform), transform, TARGET_BOUNDS, OUTPUT_STEP)
    profile = {
        "driver": "GTiff", "dtype": "float32", "count": 2, "height": out.shape[0], "width": out.shape[1],
        "crs": crs, "transform": out_transform, "compress": "deflate",
    }
    with rasterio.open(target, "w", **profile) as dst:
        dst.write(out, 1)
        dst.write(nearby, 2)
        dst.set_band_description(1, "all-sky light pollution ratio (ALR)")
        dst.set_band_description(2, "mean radiance within 1 km (nW/cm2/sr)")
    quantiles = np.quantile(out, [0, 0.05, 0.25, 0.5, 0.75, 0.95, 1])
    print(f"{out.shape[1]} x {out.shape[0]} 칸. ALR 분위수(0, 5, 25, 50, 75, 95, 100%): "
          + ", ".join(f"{q:.2f}" for q in quantiles))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
