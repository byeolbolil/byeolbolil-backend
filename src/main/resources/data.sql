-- 명소 데이터. 2026-11-25에 다시 골랐다(docs/decisions.md "명소 목록 재선정", 조사 기록은 docs/issues/spots.md).
--
-- 실행할 때마다 아래 값으로 덮어쓴다. 명소 정보는 이 파일이 원본이라서, 고치려면 여기를 고치고 배포한다.
-- kind가 OPEN_SITE인 곳은 밤에 자유롭게 가서 보는 관측지이고 밤 추천의 후보가 된다. FACILITY는 프로그램으로
-- 망원경을 보는 천문대와 과학관이고 추천 후보가 아니다.
-- 좌표는 viewpoint에 적은 주차장이나 전망대의 것이다. OpenStreetMap에서 확인했고, 12번과 15번은 위키백과의 좌표다.
-- 8번, 10번, 14번은 차를 대는 자리를 지도에서 특정하지 못해 대략의 위치다(8번과 10번은 그 길 위의 한 점, 14번은
-- 한국관광공사의 해변 좌표). 이런 곳은 guide에 그렇게 적고 참고 링크로 자리를 찾게 한다.
-- light_pollution_level은 비워 둔다. 예전 값은 틀린 좌표로 매긴 것이고, 광해 등급은 좌표로 그때그때 찾는다.
INSERT INTO observation_spot
    (id, title, address, latitude, longitude, description, light_pollution_level,
     is_parking_available, is_car_access, is_restroom_available, kind, status, viewpoint, guide, caution)
VALUES
    (1, '강릉 안반데기', '강원특별자치도 강릉시 왕산면 대기리 (멍에전망대)', 37.62633, 128.73942, '해발 1,100m 고랭지 배추밭 위의 고원이에요. 맨눈으로 은하수가 보이는 곳으로 가장 널리 알려져 있어요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '멍에전망대 주차장', '멍에전망대 주차장에 대요. 40면쯤이라 주말에는 모자라요. 무료이고 주차장에 화장실이 있어요. 올라가는 산길이 좁고 가팔라요.', '농로에는 주차와 차박이 금지예요. 주민들이 배추를 실어 내는 길이라 주차장만 써 주세요. 텐트와 취사도 안 돼요.'),
    (2, '평창 육백마지기', '강원특별자치도 평창군 미탄면 청옥산길 (청옥산 정상부)', 37.4087, 128.5104, '청옥산 정상부의 너른 초원이에요. 주차장에서 내리면 바로 사방이 트여요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '육백마지기 주차장', '정상부 주차장까지 차로 올라가요. 무료이고 화장실이 있어요.', '2026년 11월 30일부터 2027년 4월 1일까지 주차장과 화장실을 닫고, 평창군이 방문을 삼가 달라고 해요. 취사와 야영은 금지예요.'),
    (3, '영월 별마로천문대', '강원도 영월군 영월읍 천문대길 397', 37.19339, 128.49547, '봉래산 정상의 시민천문대예요.', NULL, false, false, true, 'FACILITY', 'ACTIVE', NULL, '예약한 회차의 셔틀버스로만 올라가요. 2024년 3월부터 개인 차량은 정상에 갈 수 없어요.', NULL),
    (4, '화천 조경철천문대', '강원도 화천군 사내면 천문대길 431', 38.11869, 127.43406, '광덕산 해발 1,010m 능선의 천문대예요. 천문대가 문을 닫은 뒤에도 주차장에서 별을 보는 사람들이 모여요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '천문대 주차장', '천문대 앞까지 차로 가요. 주차장과 화장실이 있어요. 천문대는 오후 2시부터 밤 10시까지 열고 월요일은 쉬어요.', '외길에 급경사가 있고 겨울에는 얼어서 통제될 수 있어요. 야영과 취사는 금지예요. 유성우 때는 주차장이 가득 차요.'),
    (5, '정선 타임캡슐공원', '강원도 정선군 신동읍 엽기소나무길 518-23', 37.201246, 128.688059, '새비재 해발 850m 언덕의 공원이에요. 영화에 나온 소나무와 은하수를 함께 찍는 곳으로 알려져 있어요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '타임캡슐공원', '공원 주차장에 대요. 밤에는 화장실과 카페가 닫혀 있어요.', '공식 운영 시간은 오전 9시부터 오후 6시이고 12월부터 3월까지는 운영을 쉬어요. 근처 리조트 쪽 하늘은 밝아요.'),
    (7, '양구 국토정중앙천문대', '강원도 양구군 남면 국토정중앙로 127', 38.06894, 128.02979, '천문대와 야영장이 함께 있어요. 별이 잘 보이는 캠핑장으로 소개돼요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '천문대 프로그램으로 관측해요. 야영장은 4월부터 11월까지 예약제로 열어요.', NULL),
    (8, '고성 미시령 옛길', '강원특별자치도 고성군 토성면 원암리 (미시령옛길)', 38.21357, 128.46791, '울산바위를 정면으로 올려다보는 옛 고갯길이에요. 바위 위로 은하수가 서는 사진으로 널리 알려져 있어요.', NULL, false, true, false, 'OPEN_SITE', 'ACTIVE', '울산바위 전망대 쉼터', '델피노 리조트 쪽에서 옛길을 오르다 보면 벤치가 있는 쉼터가 나와요. 그 근처 길가에 차를 대요. 화장실은 없어요. 지도의 위치는 대략이라 정확한 자리는 아래 글에서 확인하세요.', '쉼터 가로등이 밤늦게까지 켜져 있고 속초 쪽 하늘은 밝아요. 겨울에는 눈 때문에 옛길을 막는 해가 많으니 가기 전에 통제 여부를 확인하세요. 은하수가 바위 위에 오는 때는 5~6월이에요.'),
    (9, '가평 화악터널 쌈지공원', '경기도 가평군 북면 화악리 산228-1', 37.99708, 127.52636, '화악산 화악터널 앞 고갯마루의 작은 공원이에요. 수도권에서 가까운 별 보기 장소로 방송에 나온 뒤 유명해졌어요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '쌈지공원 전망대', '공원 앞 무료 주차장에 대요. 자리가 적어요. 24시간 열려 있어요.', '도로 가로등이 밝다는 후기가 많아요. 전망대 쪽에서 가로등을 등지고 보세요. 겨울에는 주차장이 막혀 있을 때가 있어요.'),
    (10, '양평 벗고개', '경기도 양평군 양동면 금왕리 187 (벗고개터널)', 37.503, 127.7308, '양동면 산골 고갯마루의 작은 터널이에요. 터널 안에서 반원 모양으로 보이는 밤하늘 사진으로 알려져 있어요.', NULL, false, true, false, 'OPEN_SITE', 'ACTIVE', '벗고개터널 앞', '주차장이 없어 터널 근처 길가에 대요. 화장실과 가게가 없어요. 지도에서 벗고개를 찾으면 서종면의 같은 이름 고개가 함께 나오니 양동면 금왕리 쪽으로 가세요.', '차가 다니는 길이라 걸을 때 조심하세요. 들어설 때는 먼저 온 사람들을 위해 전조등을 줄이고 천천히 가 주세요.'),
    (11, '양평 중미산천문대', '경기도 양평군 옥천면 중미산로 1268', 37.58173, 127.45941, '해설을 들으며 망원경으로 보는 사설 천문대예요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '전부 예약제이고 유료예요. 예약한 사람만 주차하고 들어가요.', NULL),
    (12, '연천 당포성', '경기도 연천군 미산면 동이리 778', 38.0275, 127.01278, '임진강 절벽 위의 고구려 성터예요. 주변에 민가가 없어 수도권에서 별 보기 좋은 곳으로 꼽혀요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '당포성 주차장', '주차장에서 5~7분 걸어요. 자리가 적어요. 주차장에 화장실이 있고 늘 열려 있어요.', '은하수보다는 별자리와 유성우를 보기 좋은 곳이에요. 다른 사람을 위해 헤드라이트와 플래시를 꺼 주세요.'),
    (13, '제천 덕주산성', '충청북도 제천시 한수면 미륵송계로 1405', 36.86206, 128.09643, '월악산 자락 도로변에 성문이 남아 있는 산성이에요. 성문 위로 뜨는 은하수 사진으로 알려져 있어요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '덕주산성 앞 주차장', '성문 앞 도로변 주차장에 대요.', '월악산국립공원 구역이에요. 밤에는 탐방로로 들어가지 말고 성문 주변에서 보세요. 은하수는 5월이 지나야 성 위로 올라와요.'),
    (14, '태안 운여해변', '충청남도 태안군 고남면 장삼포로 535-57', 36.43358, 126.35595, '안면도 서쪽의 한적한 해변이에요. 방파제 위 솔숲(솔섬)이 물에 비치는 풍경과 별을 함께 찍는 곳으로 알려져 있어요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '솔섬 방파제 앞', '해변 주차장에 대고 남쪽 끝의 솔숲 방파제로 걸어가요. 주차장과 화장실이 있지만 자리가 넉넉하지 않아요. 지도의 위치는 해변 입구쯤이에요.', '솔숲이 물에 비치는 모습은 밀물 때만 보여요. 가기 전에 물때를 확인하세요. 맨눈으로 은하수가 보이는지는 후기가 엇갈려요. 파도가 높은 날에는 물가에 가까이 가지 마세요.'),
    (15, '부여 성흥산성', '충청남도 부여군 임천면 성흥로97번길 167', 36.19556, 126.89833, '가림성이라고도 하는 백제 산성이에요. 성벽 위의 큰 느티나무(사랑나무)와 별을 함께 찍는 곳으로 유명해요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '사랑나무', '정상 아래 무료 주차 공간에 대고 계단과 오르막을 10~20분 걸어요. 올라가는 길이 좁고 가팔라요.', '해가 지면 계단이 금방 어두워져요. 랜턴을 챙기세요. 아래 마을 쪽 하늘은 밝아요.'),
    (16, '대전 시민천문대', '대전광역시 유성구 과학로 213-48', 36.38004, 127.35376, '대덕연구단지 안의 도심 시민천문대예요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '오후 2시부터 밤 10시까지 무료로 자유롭게 관람해요. 월요일은 쉬어요.', NULL),
    (17, '청양 칠갑산천문대', '충청남도 청양군 정산면 한티고개길 178-46', 36.43375, 126.88987, '칠갑산의 군립 천문대예요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '홈페이지에서 예약해야 들어가요. 월요일은 쉬어요. 주차장에서 5분 걸어요.', NULL),
    (18, '영양 반딧불이생태공원', '경상북도 영양군 수비면 반딧불이로 129', 36.83047, 129.26906, '2015년 아시아에서 처음 국제밤하늘보호공원으로 지정된 곳이에요. 밤에 조명을 켜지 않아 은하수가 잘 보여요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '반딧불이천문대 앞 주차장', '공원은 늘 열려 있고 무료예요. 천문대 앞 주차장에 대요. 천문대 프로그램은 따로 예약해요.', '손전등과 휴대폰 화면을 줄여 주세요. 여름에는 사람이 많아요.'),
    (19, '합천 황매산', '경상남도 합천군 가회면 둔내리 산 219', 35.48649, 127.9843, '해발 800m대까지 차로 오르는 너른 평원이에요. 합천군이 은하수 명소로 알리는 곳이에요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '황매산군립공원 주차장', '군립공원은 24시간 들어갈 수 있어요. 주차는 유료이고 직원이 없으면 무인 정산기를 써요.', '정상 주차장의 유도등이 밤 10시까지 켜져 있어요. 철쭉제 기간에는 길이 통제돼요.'),
    (20, '영천 보현산', '경상북도 영천시 화북면 정각리 산 6-3', 36.16667, 128.98058, '정상에 한국천문연구원 천문대가 있는 산이에요. 천문대 옆 정상 주차장이 은하수 촬영지로 쓰여요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '정상 주차장', '정상 주차장까지 차로 올라가요. 무료예요. 천문대 전시관은 낮에만 열어요.', '도로에 일몰 후 출입금지 표지가 있고 겨울에는 통제돼요. 연구 시설 옆이라 전조등과 불빛을 꺼 주세요. 차박은 안 돼요.'),
    (21, '경주 풍력발전단지', '경상북도 경주시 문무대왕면 불국로 1056-185', 35.75048, 129.36411, '토함산 자락 능선의 풍력발전단지예요. 전망대 경풍루가 있어요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '경풍루 주차장', '늘 열려 있고 무료예요. 주차장에 40~50대가 서요. 화장실 같은 시설은 적어요.', '별 보는 곳으로 소개되지만 동호인의 관측 후기는 확인하지 못했어요.'),
    (22, '의령 한우산', '경상남도 의령군 궁류면 벽계리 (한우산 정상부)', 35.39736, 128.19308, '정상 가까이까지 차로 오르는 산이에요. 사방이 트이고 빛 공해가 적어 사진가들에게 알려져 있어요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '한우산 생태숲 주차장', '평일에는 정상부 주차장까지 차로 가요. 2026년 8월에 관측 시설과 숙소가 있는 한우산 별천지가 문을 열었어요.', '주말과 공휴일에는 쇠목재부터 차량이 통제될 수 있어요. 그때는 30분쯤 걸어요.'),
    (23, '김해 천문대', '경상남도 김해시 가야테마길 254', 35.25317, 128.88748, '분성산 정상의 시민천문대예요. 김해 시내 야경이 내려다보여요.', NULL, false, false, true, 'FACILITY', 'ACTIVE', NULL, '주차장이 없어 일반 차량은 올라갈 수 없어요. 가야테마파크 쪽에서 20분쯤 걸어요. 프로그램은 유료예요.', NULL),
    (24, '남원 항공우주천문대', '전라북도 남원시 노암동 739', 35.39917, 127.38738, '춘향테마파크 위쪽의 시립 천문대예요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '정해진 시간의 유료 프로그램으로 관측해요. 월요일은 쉬어요. 주차는 무료예요.', NULL),
    (25, '무주 반디별천문과학관', '전라북도 무주군 설천면 무설로 1324', 36.01281, 127.75846, '반디랜드 안의 천문과학관이에요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '당일 현장에서 표를 사고 정해진 회차에 관측해요. 월요일은 쉬어요.', NULL),
    (26, '고흥 우주천문과학관', '전라남도 고흥군 도양읍 장기산선창길 273-59', 34.55253, 127.12506, '산 정상의 군립 천문과학관이에요. 800mm 망원경이 있어요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '오후 2시부터 밤 10시까지 열고 유료예요. 월요일은 쉬어요. 주차는 무료예요.', NULL),
    (28, '서귀포 1100고지', '제주특별자치도 서귀포시 1100로 1555', 33.35766, 126.46275, '한라산 중턱 해발 1,100m의 휴게소예요. 제주에서 차로 갈 수 있는 대표적인 별 보기 장소예요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '1100고지 휴게소 주차장', '1100도로변 휴게소 주차장에 대요.', '안개가 자주 껴서 허탕 치는 날이 있어요. 가로등 없는 산길이고 눈이 오면 통제돼요. 맞은편 습지 탐방로는 밤에 들어가지 마세요.'),
    (29, '제주 별빛누리공원', '제주특별자치도 제주시 선돌목동길 60', 33.4447, 126.5492, '제주시의 천문 공원이에요. 관측실과 투영관이 있어요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '오후에 열어 밤까지 운영하고 유료예요. 월요일은 쉬어요. 개인은 현장에서 접수해요.', NULL),
    (30, '제주 새별오름', '제주특별자치도 제주시 애월읍 봉성리 산 59-8', 33.36366, 126.35893, '제주 서쪽 들판에 솟은 오름이에요. 주차장에서도 별이 보인다고 소개돼요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '새별오름 주차장', '입구의 무료 주차장에 대요. 늘 열려 있어요.', '조명이 없어 밤에는 능선에 오르지 말고 주차장 주변에서 보세요.'),
    (33, '송암스페이스센터', '경기도 양주시 장흥면 권율로185번길 103', 37.73588, 126.94281, '케이블카로 올라가는 사설 천문대예요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '주말에 유료 프로그램으로 운영해요.', '운영 요일이 자료마다 달라요. 가기 전에 확인하세요.'),
    (34, '국립과천과학관 천문대', '경기도 과천시 상하벌로 110', 37.43853, 127.0062, '국립과천과학관의 천체관측소예요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '토요일 저녁 6시부터 밤 10시까지 야간 프로그램을 열어요. 강연과 투영관은 예약해요.', NULL),
    (36, '포천 아트밸리 천문과학관', '경기도 포천시 신북면 아트밸리로 234', 37.92478, 127.24334, '폐채석장을 꾸민 포천아트밸리 안의 천문과학관이에요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '아트밸리 입장권이 필요하고 입장 마감이 있어요. 야간 관측은 밤까지 여는 날에만 해요.', '2026년 4월에 임시 휴관 공지가 있었어요. 가기 전에 운영 여부를 확인하세요.'),
    (40, '함백산 만항재', '강원도 정선군 고한읍 함백산로 865', 37.14863, 128.90208, '해발 1,330m 고갯마루예요. 주차장 옆으로 시야가 트여 은하수 촬영지로 꼽혀요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '만항재쉼터 주차장', '414번 지방도 고갯마루의 쉼터 주차장에 대요. 자리가 넉넉하지 않아요.', '겨울에는 눈과 결빙으로 자주 통제돼요. 산안개가 끼면 보이지 않아요.'),
    (47, '증평 좌구산천문대', '충청북도 증평군 증평읍 솟점말길 187', 36.70366, 127.65315, '좌구산 휴양랜드의 군립 천문대예요. 356mm 굴절망원경이 있어요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '야간 회차 프로그램으로 운영하고 예약을 권해요. 월요일은 쉬어요.', NULL),
    (52, '밀양 아리랑우주천문대', '경상남도 밀양시 밀양대공원로 86', 35.50259, 128.76109, '밀양 시가지 옆 공원의 시립 천문대예요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '야간에는 사전 예약한 회차에만 들어가요. 월요일은 쉬어요.', NULL),
    (53, '거창 감악산', '경상남도 거창군 신원면 덕산리 산 57', 35.59107, 127.9202, '정상부 고원에 풍력발전기와 꽃밭이 있는 별바람언덕이에요. 맨눈으로 은하수를 봤다는 후기가 있어요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '별바람언덕 주차장', '정상 주차장까지 포장도로예요. 늘 열려 있고 무료예요.', '가을 꽃 축제 기간에는 정상 주차장이 예약제예요. 전망대 외벽 조명이 밤에 켜지고 북쪽 거창 읍내 쪽은 밝아요. 지정된 곳 밖에서는 텐트와 차박이 안 돼요.'),
    (60, '장흥 정남진천문과학관', '전라남도 장흥군 장흥읍 평화우산길 180-608', 34.65801, 126.92583, '억불산 자락의 군립 천문과학관이에요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '오후 2시부터 밤 10시까지 열어요. 예약 없이 온 순서대로 관람해요. 월요일은 쉬어요. 주차는 무료예요.', NULL),
    (63, '제주 서귀포천문과학문화관', '제주특별자치도 서귀포시 1100로 506-1', 33.28883, 126.46039, '1100도로변의 시립 천문과학관이에요. 겨울에 노인성을 볼 수 있다고 알려요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '화요일부터 일요일까지 오후 2시부터 밤 10시까지 예약제 회차로 운영해요.', NULL),
    (64, '제주 용눈이오름', '제주특별자치도 제주시 구좌읍 종달리 산 28', 33.46314, 126.83165, '능선이 낮고 완만한 제주 동쪽의 오름이에요. 주변에 밝은 빛이 없어요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '용눈이오름 주차장', '무료 주차장에 대요. 정상까지 걸어서 20분쯤이에요.', '훼손 때문에 2021년부터 2023년까지 탐방이 막혔던 곳이에요. 가기 전에 통제 공지를 확인하세요.'),
    (65, '합천 오도산', '경상남도 합천군 묘산면 산제리 (KT 오도산 중계소)', 35.67441, 128.0751, '해발 1,134m 정상의 전망대예요. 사방이 트여 은하수와 운해 촬영지로 알려져 있어요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '오도산 전망대', '정상까지 가파르고 좁은 임도를 10km쯤 올라가요. 정상에 10여 대를 댈 수 있어요.', '길이 좁고 굽어 밤 운전이 어려워요. 난간이 부족하고 바람이 세요.'),
    (66, '정선 문치재', '강원특별자치도 정선군 화암면 화암리 산269', 37.36563, 128.79835, '굽이굽이 이어지는 고갯길을 내려다보는 해발 732m 고갯마루 전망대예요. 여름 은하수 명소로 소개돼요.', NULL, NULL, true, false, 'OPEN_SITE', 'ACTIVE', '문치재 전망대', '일반 도로의 고갯마루예요. 전망대에 차를 몇 대 댈 수 있는지는 확인하지 못했어요.', '도로가 좁고 중간에 차를 세울 곳이 마땅치 않아요.'),
    (67, '제주 마방목지', '제주특별자치도 제주시 516로 (용강동)', 33.42704, 126.60387, '5·16도로 옆의 너른 목장 초지예요. 밤에는 인적이 끊겨 별 보기 좋은 곳으로 소개돼요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '마방목지 주차장', '도로변 주차장에 대요.', '소개한 자료가 2018년 것이라 지금의 밤 주차 여건은 확인하지 못했어요. 초지 안으로는 들어가지 마세요.')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    address = VALUES(address),
    latitude = VALUES(latitude),
    longitude = VALUES(longitude),
    description = VALUES(description),
    light_pollution_level = VALUES(light_pollution_level),
    is_parking_available = VALUES(is_parking_available),
    is_car_access = VALUES(is_car_access),
    is_restroom_available = VALUES(is_restroom_available),
    kind = VALUES(kind),
    status = VALUES(status),
    viewpoint = VALUES(viewpoint),
    guide = VALUES(guide),
    caution = VALUES(caution);

-- 목록에서 뺀 곳. 그 명소를 저장해 둔 북마크가 있을 수 있어 지우지 않고 감춘다. 새로 만든 DB에는 이 행들이 없다.
UPDATE observation_spot
SET status = 'RETIRED'
WHERE id IN (6, 27, 31, 32, 35, 37, 38, 39, 41, 42, 43, 44, 45, 46, 48, 49, 50, 51, 54, 55, 56, 57, 58, 59, 61, 62);

-- 명소별 참고 링크. 가는 길과 서는 자리를 더 자세히 적은 기사, 관광 안내, 후기이고 시설은 공식 페이지다.
-- 2026-11-25에 열리는 것을 확인했다. 이 표는 통째로 이 파일이 원본이라 매번 비우고 다시 넣는다.
-- image_url은 그 글이 대표 이미지(og:image)로 내건 주소다. 링크의 미리보기로만 쓰고 파일을 옮겨 오지 않는다.
-- 사이트 로고이거나 그 장소와 상관없어 보이는 사진은 비웠다.
DELETE FROM observation_spot_link;
INSERT INTO observation_spot_link (spot_id, position, title, url, image_url)
VALUES
    (1, 0, '해발 1,100m에서 맨눈으로 은하수 보는 겨울 명소', 'https://www.ardentnews.co.kr/news/articleView.html?idxno=10300', 'https://cdn.ardentnews.co.kr/news/thumbnail/202601/10300_45700_5624_v150.jpg'),
    (1, 1, '겨울 차박족으로 몸살 앓는 별의 성지 안반데기', 'https://v.daum.net/v/Q16NWPhVPS', 'https://img1.daumcdn.net/thumb/S800x400/?fname=https://t1.daumcdn.net/news/202311/19/traveltoktok/20231119091009254hsms.jpg'),
    (2, 0, '평창문화관광: 육백마지기', 'https://tour.pc.go.kr/Home/H20000/H20100/H20101/html', NULL),
    (2, 1, '평창군, 청옥산 야생화생태단지 동절기 운영 중단', 'https://www.gukjenews.com/news/articleView.html?idxno=3758669', NULL),
    (4, 0, '화천의 밤이 들려준 별들의 속삭임', 'https://www.kado.net/news/articleView.html?idxno=1229206', 'https://cdn.kado.net/news/photo/202402/1229206_664119_3755.jpg'),
    (4, 1, '은하수가 아름답게 수를 놓는 광덕산의 밤하늘', 'https://blog.sjkimphotos.com/879', 'https://img1.daumcdn.net/thumb/R800x0/?scode=mtistory2&fname=https%3A%2F%2Fblog.kakaocdn.net%2Fdna%2FqVJwc%2Fbtq3miqR1iI%2FAAAAAAAAAAAAAAAAAAAAAA7XCItQ21VkMuNFof7h__DitC8a2CBdLQ7BR0tV69aE%2Fimg.jpg%3Fcredential%3DyqXZFxpELC7KVnFOS48ylbz2pIh7yKj8%26expires%3D1796050799%26allow_ip%3D%26allow_referer%3D%26signature%3Dp6HXicoAP9jRHmguA3tHivVjw2c%253D'),
    (5, 0, '정선 타임캡슐공원, 은하수 담으려는 발길', 'https://www.kwnews.co.kr/article/20261020500607', 'https://kbhvfnyo14945.edge.naverncp.com/data2/content/image/2026/10/20/.cache/512/20261020500604.jpg'),
    (7, 0, '대한민국 구석구석: 양구 국토정중앙천문대 캠핑장', 'https://korean.visitkorea.or.kr/detail/rem_detail.do?cotid=58258363-4587-457b-8e8f-a4079ad6366d', 'https://cdn.visitkorea.or.kr/img/call?cmd=VIEW&id=fb143057-7c65-4b6c-badd-8ec303f0446e'),
    (8, 0, '울산바위 은하수 포인트 가는법 정리', 'https://gall.dcinside.com/mgallery/board/view/?id=digitalpicture&no=74118', NULL),
    (8, 1, '대한민국 구석구석: 미시령옛길 울산바위 은하수', 'https://korean.visitkorea.or.kr/detail/ms_detail.do?cotid=eee5ecee-a493-4dc0-b82f-fe8f6c08dbd7', 'https://cdn.visitkorea.or.kr/img/call?cmd=VIEW&id=2afb587c-5368-47b9-b984-bb23738e0245'),
    (9, 0, '화악터널 쌈지공원 이용 안내', 'https://m.tripinfo.co.kr/info.html?content_type_id=12&content_id=2765594', 'https://thumb2.tripinfo.co.kr/thumb.php?url=http://tong.visitkorea.or.kr/cms/resource/16/2766316_image2_1.png?OPT=proxy'),
    (10, 0, '별헤는 밤, 양평으로의 초대', 'https://www.imaeil.com/page/view/2025052913575170602', 'https://www.imaeil.com/photos/2025/05/29/2025052914073106223_l.jpg'),
    (10, 1, '은하수를 맨눈으로 감상할 수 있는 명소 BEST5', 'https://www.allurekorea.com/?p=202184', 'https://www.allurekorea.com/wp_data/allure/2023/04/style_643fa135e7781.jpeg'),
    (11, 0, '중미산천문대 홈페이지', 'http://starwww.astrocafe.co.kr/', NULL),
    (12, 0, '국경 지키던 연천 당포성, 별 찾는 명소로 변신', 'https://v.daum.net/v/20260620071317204', 'https://img1.daumcdn.net/thumb/S1200x630/?fname=https://t1.daumcdn.net/news/202606/20/yonhap/20260620071317854jhhi.jpg'),
    (12, 1, '연천 당포성 별 보기 좋은 명소로 각광', 'https://www.kgnews.co.kr/news/article.html?no=663306', 'https://www.kgnews.co.kr/data/photos/20210834/art_16296952710708_476a0f.jpg'),
    (13, 0, '뚜벅이들을 위한 은하수지도: 덕주산성', 'https://gall.dcinside.com/board/view/?id=dcbest&no=6620', NULL),
    (14, 0, '태안 운여해변, 물때 따라 달라지는 노을 풍경', 'https://news-wa.com/article/travel/tour-korea/2026/11/11/20261111500063', 'https://img.news-wa.com//img/upload/2026/11/11/NWC_20261111150301.jpg'),
    (14, 1, '태안 운여해변 은하수', 'https://m.cafe.daum.net/truepicture/Qt7/1305170', 'https://t1.daumcdn.net/cafeattach/mEr9/83782d1349a1cf580316b362bedf3a67d153c307'),
    (15, 0, '부여 성흥산 사랑나무, 가림성 주차와 성곽길 동선', 'https://thetravelnews.co.kr/2026/08/buyeo-seongheungsan-love-tree-garimseong-sunset-guide/', 'https://img.thetravelnews.co.kr/2026/08/buyeo-seongheungsan-love-tree-garimseong.png'),
    (16, 0, '대전시민천문대 홈페이지', 'https://djstar.kr/', NULL),
    (17, 0, '칠갑산천문대 홈페이지', 'https://www.cheongyang.go.kr/star.do', NULL),
    (18, 0, '경북 영양군, 수하 밤하늘 별빛 명소로 인기', 'https://www.ajunews.com/view/20160217110415168', 'https://image.ajunews.com/content/image/2016/02/17/20160217110441740349.jpg'),
    (18, 1, '영양 국제밤하늘보호공원 이용 안내', 'https://www.ktriptips.com/kor/tourspot/2704122', 'https://tong.visitkorea.or.kr/cms/resource/59/3586259_image2_1.jpg'),
    (19, 0, '황매산의 오월, 은하수를 보다', 'https://www.khan.co.kr/article/201805281526001', 'https://img.khan.co.kr/news/2018/05/28/l_2018052801003406100269261.jpg'),
    (19, 1, '합천군, 황매산 은하수 별빛 야행', 'https://www.kntoday.co.kr/news/articleView.html?idxno=192751', 'https://cdn.kntoday.co.kr/news/thumbnail/202209/192751_193837_5516_v150.jpg'),
    (20, 0, '가을밤은 낮보다 황홀했네', 'https://m.seoul.co.kr/news/2012/10/04/20121004500013', 'https://img.seoul.co.kr/img/upload/2012/10/04/SSI_20121004143918.jpg'),
    (21, 0, '경주 풍력발전소, 경풍루 전망대와 산책 코스', 'https://www.telltrip.com/domestic-travel/gyeongju-wind-power-plant-hill-of-wind/', 'https://www.telltrip.com/wp-content/uploads/2025/07/gyeongju-wind-power-plant-hill-of-wind1.jpg'),
    (21, 1, '경주 풍력발전(바람의언덕) 이용 안내', 'https://www.ktriptips.com/kor/tourspot/2708718', 'https://tong.visitkorea.or.kr/cms/resource/63/4103863_image2_1.JPG'),
    (22, 0, '의령군, 은하수 체험 복합공간 한우산 별천지 준공', 'https://www.sisajournal.com/news/articleView.html?idxno=362781', NULL),
    (23, 0, '김해천문대 홈페이지', 'https://astro.ghct.or.kr', NULL),
    (28, 0, '대한민국 구석구석: 제주의 별 헤는 밤', 'https://korean.visitkorea.or.kr/detail/rem_detail.do?cotid=054247db-9860-480e-83ea-899b19fb8b53', NULL),
    (30, 0, '대한민국 구석구석: 제주의 별 헤는 밤', 'https://korean.visitkorea.or.kr/detail/rem_detail.do?cotid=054247db-9860-480e-83ea-899b19fb8b53', NULL),
    (30, 1, '10월 한 달 은빛으로 물드는 억새 트레킹 코스', 'https://www.newskrw.com/news/articleView.html?idxno=53184', 'https://cdn.newskrw.com/news/photo/202609/53184_99292_5544.jpg'),
    (33, 0, '대한민국 구석구석: 송암스페이스센터', 'https://korean.visitkorea.or.kr/detail/ms_detail.do?cotid=107300bc-7923-45cf-81bf-679c441f3d2c', 'https://cdn.visitkorea.or.kr/img/call?cmd=VIEW&id=7210e4a0-a842-4690-8c3e-a46db2691a13'),
    (36, 0, '포천아트밸리 천문과학관 홈페이지', 'https://artvalley.pcfac.or.kr/star/index.html', 'https://artvalley.pcfac.or.kr/star/images/ogv2.jpg'),
    (40, 0, '사진 찍으러 가기 좋은 은하수 명소와 맛집 4', 'https://www.esquirekorea.co.kr/article/61348', 'https://www.esquirekorea.co.kr/resources_old/online/org_thumnail_image/eq/b0fb136f-f366-474b-8475-bd673d65f9dd.jpg'),
    (40, 1, '은하수가 흐르는 밤, 한국 별 관측 성지 6', 'https://www.thetrippick.com/news/articleView.html?idxno=2752', 'https://cdn.thetrippick.com/news/photo/202602/2752_11660_2756.jpg'),
    (47, 0, '좌구산천문대 관람 안내', 'https://jp.go.kr/star/sub02_01.do', NULL),
    (53, 0, '거창 감악산 별바람 언덕', 'https://www.kyeongin.com/article/1768395', 'https://wimg.kyeongin.com/news/cms/2026/08/03/news-p.v1.20260729.47c84d555f62428fb5d380b844fe5a24_R.jpg'),
    (60, 0, '정남진천문과학관 관람 시간표', 'https://www.jangheung.go.kr/star/guide/schedule', 'https://www.jangheung.go.kr/contents/fb_star_logo.png?1234567'),
    (63, 0, '서귀포천문과학문화관 이용 안내', 'https://culture.seogwipo.go.kr/astronomy/show/intro.htm', NULL),
    (64, 0, '제주 밤에 즐기는 별빛 오름 트래킹 투어', 'https://www.waug.com/ko/magazines/1481', 'https://d2mgzmtdeipcjp.cloudfront.net/files/magazine/2025/09/25/4303661673919.jpg'),
    (64, 1, '용눈이오름 또 아프지 않게, 제주 관리지표 만든다', 'https://www.khan.co.kr/article/202402282056015', 'https://img.khan.co.kr/news/2024/02/28/l_2024022901000838000079641.jpg'),
    (65, 0, '운해와 일출을 한눈에 보는 경남 숨은 명소', 'https://www.wikitree.co.kr/articles/1142041', 'https://cdnweb01.wikitree.co.kr/webdata/editor/202606/18/202606181119161321.jpg'),
    (65, 1, '교외 산이 부른다: 합천 오도산', 'https://knnews.co.kr/news/articleView.php?idxno=399400', NULL),
    (66, 0, '정선 문치재, 해발 732m 전망대에서 보는 고갯길', 'https://www.telltrip.com/domestic-travel/jeongseon-munchijae-drive/', 'https://www.telltrip.com/wp-content/uploads/2025/07/Jeongseon-Munchijae-Drive-1.jpg'),
    (67, 0, '낭만이 빛나는 밤에', 'https://twig24.com/news/lifestyle/trip/2018/06/28/20180628027006', 'https://img.seoul.co.kr//img/upload/2018/06/27/SSI_20180627162429.jpg');
