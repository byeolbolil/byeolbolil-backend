"""관측 지점에서 지형 너머로 직접 보이는 불빛과 지평선의 높이를 계산한다.

하늘 밝기(skyglow.py)는 지형을 보지 않는다. 대기 중에서 흩어진 빛이라 산이 가려도 천정 쪽 밝기는 거의 그대로이기
때문이다(Falchi et al. 2016, Materials and Methods). 산이 실제로 막아 주는 것은 광원에서 눈으로 곧바로 오는
빛이다(Cinzano & Falchi 2012, MNRAS 427, 3337, 3.3절: "main effects of screening are on the direct illumination by
the source"). 이 스크립트는 그 직접광을 따로 구한다.

방법
- 관측자에서 방위각 0.25도마다 선을 긋고, 선을 따라 지표 고도를 읽어 올려다본 각을 구한다. 고도는 Copernicus
  DEM GLO-30(지표면 모형이라 숲과 건물의 높이가 들어 있다)이다.
- 어떤 지점 위 10m(조명이 달린 높이로 가정한 값)를 올려다본 각이 그 앞쪽 모든 지표의 각보다 크거나 같으면 그곳의
  불빛은 보인다. 지구 곡률과 대기 굴절은 고도에서 r^2 (1 - k) / 2R 을 빼서 넣는다. k = 0.13은 측량에서 쓰는 표준
  굴절 계수다.
- 1km 안쪽은 더하지 않는다. 위성 센서의 화소가 약 750m라서 그 안에서는 불빛이 관측자의 어느 쪽에 있는지 알 수
  없다. 주차장 가로등 같은 현장 조명은 이 값에 들어 있지 않다.
- 보이는 지점의 레이디언스 L을 관측자가 보는 입체각만큼 더한다. 넓이 dA인 땅은 거리 r에서 dA / r^2 로 보이고
  극좌표에서 dA = r dr dθ 이므로 합은 L T(r) dr dθ / r 가 된다.
- T(r) = exp(-3.912 r / 26km) 는 대기가 빛을 줄이는 비율이다. 수평 시정 26km는 Falchi et al. 2016이 지도를
  계산할 때 쓴 표준 대기이고, 3.912는 시정을 소광 계수로 바꾸는 Koschmieder 식의 상수다. 해수면의 값을 고도와
  상관없이 쓰므로 높은 곳에서는 먼 불빛이 실제보다 약하게 계산된다.

결과는 절대 조도가 아니라 지점끼리 견주는 지수다. 위성이 재는 것은 위로 나가는 빛이고, 옆으로 나가는 빛의
비율은 조명 기구마다 달라서 알 수 없다.

    python terrain.py <DEM 폴더> <crop_radiance.py의 출력 .tif> <지점 CSV: id,name,lat,lon> <출력 CSV>
"""
import csv
import math
import os
import sys

import numpy as np
import rasterio

EARTH_RADIUS_M = 6_371_000.0
REFRACTION = 0.13            # 표준 대기 굴절 계수
EYE_HEIGHT_M = 1.6
MAX_RANGE_M = 100_000.0
MIN_RANGE_M = 50.0
# 불빛을 더하기 시작하는 거리. VIIRS DNB의 화소 크기(약 750m)보다 가까운 곳은 위치를 가릴 수 없다
LIGHT_MIN_RANGE_M = 1_000.0
# 가정: 조명은 지표면 모형보다 이만큼 높이 달려 있다. 가로등 높이를 어림한 값이고 근거 자료는 없다
LIGHT_HEIGHT_M = 10.0
AZIMUTH_STEP_DEG = 0.25
VISIBILITY_KM = 26.0         # Falchi et al. 2016의 표준 대기
EXTINCTION_PER_KM = 3.912 / VISIBILITY_KM   # Koschmieder
# 같은 자리라도 몇십 m 옮기면 언덕에 가려지는 정도가 달라진다. 이 거리만큼 떨어진 여덟 방향에서도 계산한다
OFFSET_M = 60.0


class Elevation:
    """1도 타일을 필요할 때 읽어 두고 좌표로 고도를 찾는다. 타일이 없는 곳(바다)은 0m다."""

    def __init__(self, folder: str):
        self.folder = folder
        self.tiles: dict[tuple[int, int], tuple[np.ndarray, object] | None] = {}

    def _tile(self, lat: int, lon: int):
        key = (lat, lon)
        if key not in self.tiles:
            name = f"Copernicus_DSM_COG_10_N{lat:02d}_00_E{lon:03d}_00_DEM.tif"
            path = os.path.join(self.folder, name)
            if os.path.exists(path):
                with rasterio.open(path) as src:
                    self.tiles[key] = (src.read(1), src.transform)
            else:
                self.tiles[key] = None
        return self.tiles[key]

    def sample(self, lat: np.ndarray, lon: np.ndarray) -> np.ndarray:
        out = np.zeros(lat.shape, dtype=np.float32)
        tile_lat = np.floor(lat).astype(int)
        tile_lon = np.floor(lon).astype(int)
        for key in set(zip(tile_lat.ravel().tolist(), tile_lon.ravel().tolist())):
            tile = self._tile(*key)
            if tile is None:
                continue
            data, transform = tile
            mask = (tile_lat == key[0]) & (tile_lon == key[1])
            cols = np.clip(((lon[mask] - transform.c) / transform.a).astype(int), 0, data.shape[1] - 1)
            rows = np.clip(((lat[mask] - transform.f) / transform.e).astype(int), 0, data.shape[0] - 1)
            out[mask] = data[rows, cols]
        return out


def ranges() -> np.ndarray:
    """선 위의 표본 거리(m). 15km까지 30m 간격, 그 뒤로는 거리의 0.2%씩 넓힌다."""
    near = np.arange(MIN_RANGE_M, 15_000.0, 30.0)
    far = [15_000.0]
    while far[-1] < MAX_RANGE_M:
        far.append(far[-1] * 1.002)
    return np.concatenate([near, np.array(far)])


def destination(lat: float, lon: float, azimuth: np.ndarray, distance: np.ndarray):
    """구면에서 방위각과 거리만큼 간 곳의 좌표."""
    phi = math.radians(lat)
    delta = distance / EARTH_RADIUS_M
    sin_phi2 = math.sin(phi) * np.cos(delta) + math.cos(phi) * np.sin(delta) * np.cos(azimuth)
    phi2 = np.arcsin(sin_phi2)
    lam2 = math.radians(lon) + np.arctan2(
        np.sin(azimuth) * np.sin(delta) * math.cos(phi), np.cos(delta) - math.sin(phi) * sin_phi2)
    return np.degrees(phi2), np.degrees(lam2)


def analyse(lat: float, lon: float, elevation: Elevation, radiance: np.ndarray, transform) -> dict:
    r = ranges()
    azimuth = np.radians(np.arange(0.0, 360.0, AZIMUTH_STEP_DEG))
    grid_lat, grid_lon = destination(lat, lon, azimuth[:, None], r[None, :])

    ground = float(elevation.sample(np.array([lat]), np.array([lon]))[0])
    eye = ground + EYE_HEIGHT_M
    terrain = elevation.sample(grid_lat, grid_lon)
    drop = r ** 2 * (1 - REFRACTION) / (2 * EARTH_RADIUS_M)
    angle = np.arctan2(terrain - eye - drop[None, :], r[None, :])

    before = np.maximum.accumulate(angle, axis=1)
    horizon_before = np.concatenate([np.full((angle.shape[0], 1), -np.inf), before[:, :-1]], axis=1)
    light_angle = np.arctan2(terrain + LIGHT_HEIGHT_M - eye - drop[None, :], r[None, :])
    visible = light_angle >= horizon_before

    cols = np.clip(((grid_lon - transform.c) / transform.a).astype(int), 0, radiance.shape[1] - 1)
    rows = np.clip(((grid_lat - transform.f) / transform.e).astype(int), 0, radiance.shape[0] - 1)
    light = np.clip(radiance[rows, cols], 0, None)

    dr = np.gradient(r)
    dtheta = math.radians(AZIMUTH_STEP_DEG)
    transmission = np.exp(-EXTINCTION_PER_KM * r / 1000.0)
    element = light * (transmission * dr / r * (r >= LIGHT_MIN_RANGE_M))[None, :] * dtheta
    direct = float((element * visible).sum())
    unobstructed = float(element.sum())

    horizon = np.degrees(before[:, -1])
    return {
        "elevation_m": round(ground),
        "direct_light": direct,
        "unobstructed_light": unobstructed,
        "horizon_mean_deg": float(horizon.mean()),
        "horizon_min_deg": float(horizon.min()),
        "horizon_max_deg": float(horizon.max()),
    }


def offsets(lat: float, lon: float):
    yield lat, lon
    for bearing in range(0, 360, 45):
        new_lat, new_lon = destination(lat, lon, np.array(math.radians(bearing)), np.array(OFFSET_M))
        yield float(new_lat), float(new_lon)


def main(dem_folder: str, radiance_path: str, points_path: str, output_path: str) -> None:
    elevation = Elevation(dem_folder)
    with rasterio.open(radiance_path) as src:
        radiance = src.read(1)
        transform = src.transform
    with open(points_path, encoding="utf-8", newline="") as f:
        points = list(csv.DictReader(f))

    fields = ["id", "name", "lat", "lon", "elevation_m", "direct_light", "direct_light_min", "direct_light_max",
              "unobstructed_light", "horizon_mean_deg", "horizon_min_deg", "horizon_max_deg"]
    with open(output_path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        for point in points:
            lat, lon = float(point["lat"]), float(point["lon"])
            results = [analyse(a, b, elevation, radiance, transform) for a, b in offsets(lat, lon)]
            centre = results[0]
            nearby = sorted(result["direct_light"] for result in results)
            row = {
                "id": point["id"], "name": point["name"], "lat": lat, "lon": lon,
                "elevation_m": centre["elevation_m"],
                # 대표값은 아홉 자리의 중앙값이다. 좌표가 가리키는 한 점이 우연히 가려지거나 트인 경우를 누른다
                "direct_light": round(nearby[len(nearby) // 2], 3),
                "direct_light_min": round(nearby[0], 3),
                "direct_light_max": round(nearby[-1], 3),
                "unobstructed_light": round(centre["unobstructed_light"], 3),
                "horizon_mean_deg": round(centre["horizon_mean_deg"], 2),
                "horizon_min_deg": round(centre["horizon_min_deg"], 2),
                "horizon_max_deg": round(centre["horizon_max_deg"], 2),
            }
            writer.writerow(row)
            f.flush()
            print(f'{row["name"]}: 고도 {row["elevation_m"]}m, 직접광 {row["direct_light"]} '
                  f'({row["direct_light_min"]}~{row["direct_light_max"]}), 가림 없을 때 {row["unobstructed_light"]}, '
                  f'지평선 평균 {row["horizon_mean_deg"]}도')


if __name__ == "__main__":
    main(*sys.argv[1:5])
