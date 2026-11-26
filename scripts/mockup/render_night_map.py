"""목업용 한국 야간광 지도 이미지를 만든다.

    git show e987548:src/main/resources/light_pollution_korea.csv > light_pollution_korea.csv
    python scripts/mockup/render_night_map.py light_pollution_korea.csv docs/mockup/korea-night.jpg

입력은 VIIRS 기반 CSV(lat, lon, radiance, bortle)다. 이전 분석 API와 함께 2026-11-27에 저장소에서 지웠으므로
그 전 커밋에서 꺼내 쓴다. 위도 33.0~38.75, 경도 124.5~131.0 범위를
가로 1100px로 그리고, 경도 방향은 cos(36°)로 보정한다. 목업의 핀 좌표 계산(mx, my)이 같은 투영을 쓰므로
범위나 크기를 바꾸면 docs/mockup/index.html의 MAPW, MAPH, PPD도 함께 바꿔야 한다.

radiance는 로그 스케일로 밝기를 정하고, 가우시안 블러 두 단계로 번짐을 만든다. 시각화용이며
등급 판정에는 쓰지 않는다.
"""
import sys, csv, math
import numpy as np
from PIL import Image, ImageFilter

src, out = sys.argv[1], sys.argv[2]
LAT0, LAT1, LON0, LON1 = 33.0, 38.75, 124.5, 131.0
W = 1100
S = 2  # 절반 해상도로 모은 뒤 확대해서 샘플링 간격 때문에 생기는 줄무늬를 없앤다
K = math.cos(math.radians(36))
ppd = W / ((LON1 - LON0) * K)
H = int(round((LAT1 - LAT0) * ppd))
w, h = W // S, H // S

rad = np.zeros((h, w), np.float32)
land = np.zeros((h, w), np.float32)
with open(src, encoding="utf-8") as f:
    r = csv.reader(f)
    next(r)
    for lat, lon, rv, _ in r:
        x = int((float(lon) - LON0) * K * ppd / S)
        y = int((LAT1 - float(lat)) * ppd / S)
        if 0 <= x < w and 0 <= y < h:
            rad[y, x] = max(rad[y, x], float(rv))
            land[y, x] = 1


def up(a):
    return np.asarray(Image.fromarray(a.astype(np.float32)).resize((W, H), Image.BILINEAR))


def blur(a, radius):
    im = Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius))
    return np.asarray(im).astype(np.float32) / 255


rad, land = up(rad), up(land)
landm = np.clip(blur(np.clip(land * 2, 0, 1), 1.5) * 1.6, 0, 1)
L = np.clip((np.log1p(rad) - math.log1p(0.6)) / (math.log1p(150) - math.log1p(0.6)), 0, 1)
glow = np.clip(blur(L, 0.7) * 0.95 + blur(L, 5) * 0.55 + blur(L, 16) * 0.35, 0, 1.3)

bg = np.array([6, 9, 22], np.float32)
landc = np.array([17, 21, 42], np.float32)
warm = np.array([255, 184, 104], np.float32)
hot = np.array([255, 244, 220], np.float32)
img = bg + (landc - bg) * landm[..., None]
g = glow[..., None]
img = img + warm * np.clip(g, 0, 1) ** 1.25 * 0.8
img = img + (hot - img) * np.clip((g - 0.8) / 0.5, 0, 1) * 0.85
Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).save(out, quality=86, optimize=True, progressive=True)
print(W, H)
