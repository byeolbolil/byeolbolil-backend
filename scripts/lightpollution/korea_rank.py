"""남한 땅의 하늘 밝기 분포를 백 칸으로 나눈 표를 만든다. 앱은 이 표로 "국내 상위 몇 %의 어두운 하늘"을 말한다.

    python korea_rank.py <skyglow_korea.bin.gz> <DEM 폴더> <출력 .csv>

하늘 밝기 등급(0.33, 2, 10, 50)은 미국 국립공원의 밤하늘을 기준으로 만든 눈금이라, 한국 본토는 거의 다 아래쪽 두 칸에
몰린다(2026-11-27 계산으로 본토에서 2배 아래인 땅은 5%). 같은 등급 안에서 이곳이 국내에서 얼마나 어두운 편인지를 따로
말하려고 만든 표다.

남한 땅을 가리는 방법
- 땅: Copernicus DEM GLO-30을 격자 간격(30초각)으로 평균해 1m보다 높은 칸. 해안선의 칸은 바다가 섞여 있다.
- 휴전선 북쪽은 군사분계선을 몇 점으로 어림한 선(MDL)으로 뺀다. 휴전선 둘레 몇 km는 어긋날 수 있다.
- 대마도, 규슈, 고토 열도는 경위도 상자로 뺀다.
- 울릉도와 독도는 DEM 타일(동경 130도까지)을 받지 않아서 빠진다.
섬은 넣는다. 본토만 세면 서해 외딴섬처럼 국내에서 가장 어두운 곳이 빠진다.

출력은 줄마다 "칸,ALR 상한"이다. 칸 k의 상한은 남한 땅의 k%가 그 값 이하라는 뜻이다. 앱은 ALR이 들어가는 첫 칸을
순위로 쓴다(1이 가장 어둡다).
"""
import glob
import gzip
import os
import struct
import sys

import numpy as np
import rasterio
from rasterio.enums import Resampling

MDL = [(125.0, 37.70), (126.65, 37.78), (126.68, 37.96), (127.0, 38.17), (127.3, 38.30), (127.7, 38.30),
       (128.0, 38.32), (128.2, 38.40), (128.36, 38.62), (131.0, 38.62)]


def main(sky_path: str, dem_dir: str, out_path: str) -> None:
    raw = gzip.open(sky_path).read()
    _, rows, cols = struct.unpack(">iii", raw[4:16])
    north, west, step = struct.unpack(">ddd", raw[16:40])
    alr = 10 ** (np.frombuffer(raw, ">i2", rows * cols, 40).reshape(rows, cols) / 1000)
    lat = north - (np.arange(rows) + 0.5) * step
    lon = west + (np.arange(cols) + 0.5) * step
    LAT, LON = np.meshgrid(lat, lon, indexing="ij")

    cells = int(round(1 / step))  # DEM 1도 타일 한 장이 격자 몇 칸인지
    elev = np.zeros((rows, cols))
    for path in glob.glob(os.path.join(dem_dir, "*.tif")):
        name = os.path.basename(path)
        tile_lat = int(name.split("_N")[1][:2])
        tile_lon = int(name.split("_E")[1][:3])
        with rasterio.open(path) as src:
            small = src.read(1, out_shape=(cells, cells), resampling=Resampling.average)
        r0 = int(round((north - (tile_lat + 1)) / step))
        c0 = int(round((tile_lon - west) / step))
        r_lo, r_hi = max(r0, 0), min(r0 + cells, rows)
        c_lo, c_hi = max(c0, 0), min(c0 + cells, cols)
        if r_lo < r_hi and c_lo < c_hi:
            elev[r_lo:r_hi, c_lo:c_hi] = small[r_lo - r0:r_hi - r0, c_lo - c0:c_hi - c0]

    south_of_mdl = LAT < np.interp(LON, [p[0] for p in MDL], [p[1] for p in MDL])
    japan = ((LON > 129.1) & (LON < 129.55) & (LAT > 34.0) & (LAT < 34.75)) \
        | ((LON > 129.55) & (LAT < 35.0)) | ((LON > 127.5) & (LAT < 34.0))
    land = (elev > 1) & south_of_mdl & ~japan
    values = alr[land]

    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("# 남한 땅의 하늘 밝기(ALR) 분포. scripts/lightpollution/korea_rank.py가 만든다\n")
        for k in range(1, 101):
            f.write(f"{k},{np.percentile(values, k):.4f}\n")
    print(f"남한 땅 {land.sum()}칸. 1% {np.percentile(values, 1):.2f}, 10% {np.percentile(values, 10):.2f}, "
          f"50% {np.percentile(values, 50):.2f}, 90% {np.percentile(values, 90):.2f}")


if __name__ == "__main__":
    main(*sys.argv[1:4])
