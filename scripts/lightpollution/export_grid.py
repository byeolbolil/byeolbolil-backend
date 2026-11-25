"""skyglow.py가 만든 격자를 앱이 읽는 파일로 바꾼다.

형식(빅엔디언, gzip으로 감쌈)
    4바이트  "SKYG"
    int32    형식 버전 (1)
    int32    줄 수, int32 칸 수
    float64  북쪽 끝 위도, float64 서쪽 끝 경도, float64 칸 간격(도)
    int16[]  하늘 밝기(ALR). 줄마다 서쪽에서 동쪽으로, 북쪽 줄부터. 값은 round(1000 * log10(ALR))
    int16[]  둘레 1km의 평균 레이디언스. 같은 순서, 같은 인코딩

두 값 모두 네 자릿수 넘게 걸쳐 있어서 로그로 저장한다. 1000을 곱한 정수의 한 단계는 0.23% 차이다.
앱에서는 `SkyBrightnessGrid`가 읽는다.

    python export_grid.py <skyglow.py의 출력 .tif> <출력 .bin.gz>
"""
import gzip
import struct
import sys

import numpy as np
import rasterio

FLOOR = 0.001  # 이보다 작은 값은 이 값으로 적는다. 불빛이 없는 곳이다


def main(source: str, target: str) -> None:
    with rasterio.open(source) as src:
        ratio = src.read(1)
        nearby = src.read(2)
        transform = src.transform
    header = b"SKYG" + struct.pack(">iii", 1, ratio.shape[0], ratio.shape[1]) \
        + struct.pack(">ddd", transform.f, transform.c, transform.a)
    with gzip.GzipFile(target, "wb", mtime=0) as f:
        f.write(header)
        for layer in (ratio, nearby):
            f.write(np.round(1000 * np.log10(np.maximum(layer, FLOOR))).astype(">i2").tobytes())
    print(f"{ratio.shape[0]} 줄 x {ratio.shape[1]} 칸, 북 {transform.f}, 서 {transform.c}, 간격 {transform.a}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
