"""VIIRS 야간 불빛 연간 합성에서 한국과 그 둘레를 잘라 낸다.

입력은 EOG의 VNL V2 연간 합성(average_masked)이다. 전 지구 파일은 11GB가 넘으므로 gzip을 풀지 않고 필요한
줄만 읽는다. 하늘 밝기는 300km 밖의 불빛까지 더해서 구하므로(Duriscoe et al. 2018) 계산할 범위보다 300km
넓게 자른다.

    python crop_radiance.py <VNL .tif 또는 .tif.gz> <출력 .tif>
"""
import sys

import numpy as np
import rasterio
from rasterio.windows import from_bounds

from grid import BUFFER_BOUNDS


def main(source: str, target: str) -> None:
    path = f"/vsigzip/{source}" if source.endswith(".gz") else source
    with rasterio.open(path) as src:
        west, south, east, north = BUFFER_BOUNDS
        window = from_bounds(west, south, east, north, src.transform).round_offsets().round_lengths()
        data = src.read(1, window=window).astype(np.float32)
        profile = {
            "driver": "GTiff",
            "dtype": "float32",
            "count": 1,
            "height": data.shape[0],
            "width": data.shape[1],
            "crs": src.crs,
            "transform": src.window_transform(window),
            "compress": "deflate",
            "tiled": True,
        }
    with rasterio.open(target, "w", **profile) as dst:
        dst.write(data, 1)
    print(f"{data.shape[1]} x {data.shape[0]} 픽셀, 최댓값 {data.max():.1f}, 0보다 큰 픽셀 {(data > 0).mean():.1%}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
