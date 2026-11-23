"""목업용 일반 지도 이미지와 도시 라벨 목록을 만든다.

    python scripts/mockup/render_base_map.py <Natural Earth geojson 폴더> docs/mockup/korea-base.jpg cities.json

필요한 Natural Earth 10m 레이어(퍼블릭 도메인): ne_10m_land, ne_10m_lakes, ne_10m_roads,
ne_10m_populated_places_simple. https://github.com/nvkelso/natural-earth-vector 의 geojson 폴더에서 받는다.
투영은 render_night_map.py와 같다. 도시 이름 한글 표기는 아래 KO 사전에 직접 적었다.
"""
import sys, json, math, pathlib
from PIL import Image, ImageDraw, ImageFilter
sys.stdout.reconfigure(encoding="utf-8")
d = pathlib.Path(sys.argv[1]); out = sys.argv[2]; cities_out = sys.argv[3]
LAT0, LAT1, LON0, LON1 = 33.0, 38.75, 124.5, 131.0
W = 1100
K = math.cos(math.radians(36)); ppd = W / ((LON1 - LON0) * K); H = int(round((LAT1 - LAT0) * ppd))
SS = 2  # 2배로 그린 뒤 축소해서 안티에일리어싱
def P(lon, lat): return ((lon - LON0) * K * ppd * SS, (LAT1 - lat) * ppd * SS)
def inbox(coords, pad=1.0):
    return any(LON0 - pad <= x <= LON1 + pad and LAT0 - pad <= y <= LAT1 + pad for x, y in coords)
def rings(geom):
    if geom["type"] == "Polygon": yield geom["coordinates"]
    elif geom["type"] == "MultiPolygon":
        for poly in geom["coordinates"]: yield poly
def lines(geom):
    if geom["type"] == "LineString": yield geom["coordinates"]
    elif geom["type"] == "MultiLineString":
        for l in geom["coordinates"]: yield l

SEA = (176, 206, 222); LAND = (243, 240, 233); COAST = (140, 172, 192); LAKE = (176, 206, 222)
im = Image.new("RGB", (W * SS, H * SS), SEA); dr = ImageDraw.Draw(im)
land = json.loads((d / "ne_10m_land.geojson").read_text(encoding="utf-8"))
for f in land["features"]:
    for poly in rings(f["geometry"]):
        if not inbox(poly[0]): continue
        dr.polygon([P(x, y) for x, y in poly[0]], fill=LAND, outline=COAST)
        for hole in poly[1:]: dr.polygon([P(x, y) for x, y in hole], fill=SEA)
lakes = json.loads((d / "ne_10m_lakes.geojson").read_text(encoding="utf-8"))
for f in lakes["features"]:
    for poly in rings(f["geometry"]):
        if inbox(poly[0], 0): dr.polygon([P(x, y) for x, y in poly[0]], fill=LAKE)
roads = json.loads((d / "ne_10m_roads.geojson").read_text(encoding="utf-8"))
for f in roads["features"]:
    t = f["properties"].get("type", "")
    major = "Major" in t
    for l in lines(f["geometry"]):
        if not inbox(l, 0): continue
        dr.line([P(x, y) for x, y in l], fill=(232, 205, 160) if major else (225, 220, 210), width=(5 if major else 3) * SS // 2, joint="curve")
im = im.resize((W, H), Image.LANCZOS)
im.save(out, quality=86, optimize=True)

KO = {"Seoul": "서울", "Busan": "부산", "Incheon": "인천", "Daegu": "대구", "Daejeon": "대전", "Gwangju": "광주",
      "Ulsan": "울산", "Suwon": "수원", "Changwon": "창원", "Cheongju": "청주", "Jeonju": "전주", "Chuncheon": "춘천",
      "Gangneung": "강릉", "Pohang": "포항", "Jeju": "제주", "Wonju": "원주", "Andong": "안동", "Mokpo": "목포",
      "Yeosu": "여수", "Gunsan": "군산", "Sokcho": "속초", "Jinju": "진주", "Gyeongju": "경주", "Cheonan": "천안"}
pp = json.loads((d / "ne_10m_populated_places_simple.geojson").read_text(encoding="utf-8"))
cities, seen = [], set()
for f in sorted(pp["features"], key=lambda f: -f["properties"]["pop_max"]):
    p = f["properties"]; n = p["name"]
    if p.get("adm0_a3") == "KOR" and n in KO and n not in seen:
        seen.add(n); cities.append([KO[n], round(p["latitude"], 3), round(p["longitude"], 3), 1 if p["pop_max"] > 1_000_000 else 2])
pathlib.Path(cities_out).write_text(json.dumps(cities, ensure_ascii=False), encoding="utf-8")
print(W, H, len(cities), [c[0] for c in cities])
