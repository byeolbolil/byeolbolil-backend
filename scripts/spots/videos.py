"""명소 영상 목록(videos.tsv)에서 data.sql의 observation_spot_video 행을 만든다.

영상마다 유튜브의 oEmbed 주소(https://www.youtube.com/oembed, 키가 필요 없다)를 불러 제목과 채널 이름을 받는다.
지워졌거나 비공개가 된 영상은 받지 못하므로 표준 오류에 적고 건너뛴다. 그 줄은 videos.tsv에서 지운다.

    python scripts/spots/videos.py > rows.sql

출력의 VALUES 줄들을 src/main/resources/data.sql의 observation_spot_video 자리에 붙인다.
제목이 바뀌었거나 영상이 사라졌는지 보려면 몇 달에 한 번 다시 돌린다.
"""
import io
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

OEMBED = "https://www.youtube.com/oembed?format=json&url="
USER_AGENT = "byeolbolil/1.0 https://www.byeolbolil.xyz"
VIDEO_ID = re.compile(r"(?:[?&]v=|/shorts/|youtu\.be/)([A-Za-z0-9_-]{11})")

# 요청 사이에 쉬는 시간(초). 수십 건을 한꺼번에 보내지 않는다
PAUSE = 0.5


def read_list(path):
    rows = []
    for line in io.open(path, encoding="utf-8"):
        line = line.rstrip("\n")
        if not line.strip() or line.startswith("#"):
            continue
        spot_id, url = line.split("\t")[:2]
        match = VIDEO_ID.search(url)
        if not match:
            raise SystemExit("영상 ID를 읽을 수 없는 주소: " + url)
        rows.append((int(spot_id), match.group(1)))
    return rows


def fetch(video_id):
    watch = "https://www.youtube.com/watch?v=" + video_id
    request = urllib.request.Request(OEMBED + urllib.parse.quote(watch, safe=""), headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.loads(response.read().decode("utf-8"))


def is_short(video_id):
    """쇼츠인지 본다. 유튜브는 쇼츠에만 세로 비율 그대로의 미리보기(oar2.jpg)를 둔다.

    문서로 밝힌 규칙은 아니고 2026-11-27에 쇼츠 다섯 개와 일반 영상 네 개로 확인한 것이다. oEmbed는 주소가
    /shorts/일 때만 세로 크기를 알려 줘서, 영상 ID만으로는 가릴 수 없다.
    """
    request = urllib.request.Request(
        "https://i.ytimg.com/vi/%s/oar2.jpg" % video_id, headers={"User-Agent": USER_AGENT}, method="HEAD")
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return response.status == 200
    except urllib.error.HTTPError:
        return False


def quote(text):
    return "'" + text.replace("\\", "\\\\").replace("'", "''") + "'"


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    rows = read_list(os.path.join(here, "videos.tsv"))
    out = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", newline="\n")
    position = {}
    lines = []
    for spot_id, video_id in rows:
        try:
            meta = fetch(video_id)
        except urllib.error.HTTPError as error:
            print("받지 못함(%d): 명소 %d, %s" % (error.code, spot_id, video_id), file=sys.stderr)
            continue
        finally:
            time.sleep(PAUSE)
        short_form = is_short(video_id)
        index = position.get(spot_id, 0)
        position[spot_id] = index + 1
        lines.append("    (%d, %d, %s, %s, %s, %s)" % (
            spot_id, index, quote(video_id), quote(meta["title"].strip()), quote(meta["author_name"].strip()),
            "TRUE" if short_form else "FALSE"))
    out.write(",\n".join(lines) + ";\n")
    out.flush()


if __name__ == "__main__":
    main()
