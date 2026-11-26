-- 명소 데이터. 2026-11-25에 다시 골랐다(docs/decisions.md "명소 목록 재선정", 조사 기록은 docs/issues/spots.md).
--
-- 실행할 때마다 아래 값으로 덮어쓴다. 명소 정보는 이 파일이 원본이라서, 고치려면 여기를 고치고 배포한다.
-- kind가 OPEN_SITE인 곳은 밤에 자유롭게 가서 보는 관측지이고 밤 추천의 후보가 된다. FACILITY는 프로그램으로
-- 망원경을 보는 천문대와 과학관이고 추천 후보가 아니다.
-- 좌표는 viewpoint에 적은 주차장이나 전망대의 것이다. OpenStreetMap에서 확인했고, 12번은 위키백과의 좌표,
-- 15번은 국가유산청이 천연기념물 "부여 가림성 느티나무"에 적어 둔 좌표다(위키백과의 좌표는 300m 아래 산자락이었다).
-- 8번, 10번, 14번은 차를 대는 자리를 지도에서 특정하지 못해 대략의 위치다(8번과 10번은 그 길 위의 한 점, 14번은
-- 한국관광공사의 해변 좌표). 이런 곳은 guide에 그렇게 적고 참고 링크로 자리를 찾게 한다.
-- 68번부터 75번은 2026-11-26에 더한 곳이다(조사 기록은 docs/issues/spots-additions.md). 68, 70, 71, 75번은
-- OpenStreetMap의 주차장과 전망대 좌표, 69번은 국가유산청이 서원에 적어 둔 좌표, 72번과 73번은 OpenStreetMap의
-- 건물과 유적 좌표, 74번은 OpenStreetMap의 정상 좌표다. 주차장을 따로 특정하지 못한 곳은 guide에 그렇게 적었다.
-- 76번부터 89번은 같은 날 한 번 더 더한 곳이다. 근거가 하나뿐이거나 밤 출입, 주차 지점을 확인하지 못한 곳도
-- 넣되 그 사실을 guide와 caution에 적었다(docs/decisions.md 2026-11-26 "보는 자리와 명소 14곳"). 좌표는 한국관광공사
-- 자료(76, 78, 80, 81, 84, 89번)와 OpenStreetMap(79, 85, 86, 88번)의 것이고, 77, 82, 83, 87번은 대략의 위치다.
-- light_pollution_level은 비워 둔다. 예전 값은 틀린 좌표로 매긴 것이고, 광해 등급은 좌표로 그때그때 찾는다.
INSERT INTO observation_spot
    (id, title, address, latitude, longitude, description, light_pollution_level,
     is_parking_available, is_car_access, is_restroom_available, kind, status, viewpoint, guide, caution)
VALUES
    (1, '강릉 안반데기', '강원특별자치도 강릉시 왕산면 대기리 (멍에전망대)', 37.62633, 128.73942, '해발 1,100m 고랭지 배추밭 위의 고원이에요. 맨눈으로 은하수가 보이는 곳으로 가장 널리 알려져 있어요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '멍에전망대 주차장', '멍에전망대 주차장에 대요. 40면쯤이라 주말에는 모자라요. 무료이고 주차장에 화장실이 있어요. 올라가는 산길이 좁고 가팔라요.', '농로에는 주차와 차박이 금지예요. 주민들이 배추를 실어 내는 길이라 주차장만 써 주세요. 텐트와 취사도 안 돼요.'),
    (2, '평창 육백마지기', '강원특별자치도 평창군 미탄면 청옥산길 (청옥산 정상부)', 37.4087, 128.5104, '청옥산 정상부의 너른 초원이에요. 주차장에서 내리면 바로 사방이 트여요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '육백마지기 주차장', '정상부 주차장까지 차로 올라가요. 무료이고 화장실이 있어요.', '2026년 11월 30일부터 2027년 4월 1일까지 주차장과 화장실을 닫고, 평창군이 방문을 삼가 달라고 해요. 취사와 야영은 금지예요. 은하수 정원을 만드는 공사로 올라가는 길을 가끔 막을 수 있어요.'),
    (3, '영월 별마로천문대', '강원도 영월군 영월읍 천문대길 397', 37.19339, 128.49547, '봉래산 정상의 시민천문대예요.', NULL, false, false, true, 'FACILITY', 'ACTIVE', NULL, '예약한 회차의 셔틀버스로만 올라가요. 2024년 3월부터 개인 차량은 정상에 갈 수 없어요.', NULL),
    (4, '화천 조경철천문대', '강원도 화천군 사내면 천문대길 431', 38.11869, 127.43406, '광덕산 해발 1,010m 능선의 천문대예요. 천문대가 문을 닫은 뒤에도 주차장에서 별을 보는 사람들이 모여요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '천문대 주차장', '천문대 앞까지 차로 가요. 주차장과 화장실이 있어요. 천문대는 오후 2시부터 밤 10시까지 열고 월요일은 쉬어요.', '외길에 급경사가 있고 겨울에는 얼어서 통제될 수 있어요. 야영과 취사는 금지예요. 유성우 때는 주차장이 가득 차요.'),
    (5, '정선 타임캡슐공원', '강원도 정선군 신동읍 엽기소나무길 518-23', 37.201246, 128.688059, '새비재 해발 850m 언덕의 공원이에요. 영화에 나온 소나무와 은하수를 함께 찍는 곳으로 알려져 있어요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '타임캡슐공원', '공원 주차장에 대요. 밤에는 화장실과 카페가 닫혀 있어요.', '공식 운영 시간은 오전 9시부터 오후 6시이고 12월부터 3월까지는 운영을 쉬어요. 근처 리조트 쪽 하늘은 밝아요.'),
    (7, '양구 국토정중앙천문대', '강원도 양구군 남면 국토정중앙로 127', 38.06894, 128.02979, '천문대와 야영장이 함께 있어요. 별이 잘 보이는 캠핑장으로 소개돼요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '천문대 프로그램으로 관측해요. 야영장은 4월부터 11월까지 예약제로 열어요.', NULL),
    (8, '고성 미시령 옛길', '강원특별자치도 고성군 토성면 원암리 (미시령옛길)', 38.21357, 128.46791, '울산바위를 정면으로 올려다보는 옛 고갯길이에요. 바위 위로 은하수가 서는 사진으로 널리 알려져 있어요.', NULL, false, true, false, 'OPEN_SITE', 'ACTIVE', '울산바위 전망대 쉼터', '델피노 리조트 쪽에서 옛길을 오르다 보면 벤치가 있는 쉼터가 나와요. 그 근처 길가에 차를 대요. 화장실은 없어요. 지도의 위치는 대략이라 정확한 자리는 아래 글에서 확인하세요.', '쉼터 가로등이 밤늦게까지 켜져 있고 속초 쪽 하늘은 밝아요. 겨울에는 눈 때문에 옛길을 막는 해가 많으니 가기 전에 통제 여부를 확인하세요. 은하수가 바위 위에 오는 때는 5~6월이에요.'),
    (9, '가평 화악터널 쌈지공원', '경기도 가평군 북면 화악리 산228-1', 37.99708, 127.52636, '화악산 화악터널 앞 고갯마루의 작은 공원이에요. 수도권에서 가까운 별 보기 장소로 방송에 나온 뒤 유명해졌어요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '쌈지공원 전망대', '공원 앞 무료 주차장에 대요. 자리가 적어요. 24시간 열려 있어요.', '도로 가로등이 밝다는 후기가 많아요. 전망대 쪽에서 가로등을 등지고 보세요. 겨울에는 주차장이 막혀 있을 때가 있어요.'),
    (10, '양평 벗고개', '경기도 양평군 양동면 금왕리 187 (벗고개터널)', 37.503, 127.7308, '양동면 산골 고갯마루의 작은 터널이에요. 터널 안에서 반원 모양으로 보이는 밤하늘 사진으로 알려져 있어요.', NULL, false, true, false, 'OPEN_SITE', 'ACTIVE', '벗고개터널 앞', '주차장이 없어 터널 근처 길가에 대요. 화장실과 가게가 없어요. 지도에서 벗고개를 찾으면 서종면의 같은 이름 고개가 함께 나오니 양동면 금왕리 쪽으로 가세요.', '차가 다니는 길이라 걸을 때 조심하세요. 들어설 때는 먼저 온 사람들을 위해 전조등을 줄이고 천천히 가 주세요.'),
    (11, '양평 중미산천문대', '경기도 양평군 옥천면 중미산로 1268', 37.58173, 127.45941, '해설을 들으며 망원경으로 보는 사설 천문대예요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '전부 예약제이고 유료예요. 예약한 사람만 주차하고 들어가요.', NULL),
    (12, '연천 당포성', '경기도 연천군 미산면 동이리 778', 38.0275, 127.01278, '임진강 절벽 위의 고구려 성터예요. 주변에 민가가 없어 수도권에서 별 보기 좋은 곳으로 꼽혀요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '당포성 주차장', '주차장에서 5~7분 걸어요. 자리가 적어요. 주차장에 화장실이 있고 늘 열려 있어요.', '은하수보다는 별자리와 유성우를 보기 좋은 곳이에요. 다른 사람을 위해 헤드라이트와 플래시를 꺼 주세요.'),
    (13, '제천 덕주산성', '충청북도 제천시 한수면 미륵송계로 1405', 36.86206, 128.09643, '월악산 자락 도로변에 성문이 남아 있는 산성이에요. 성문 위로 뜨는 은하수 사진으로 알려져 있고, 사방이 산이라 주변 불빛이 거의 들어오지 않아요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '덕주산성 앞 주차장', '성문 앞 도로변 주차장에 대요.', '월악산국립공원 구역이에요. 밤에는 탐방로로 들어가지 말고 성문 주변에서 보세요. 은하수는 5월이 지나야 성 위로 올라와요.'),
    (14, '태안 운여해변', '충청남도 태안군 고남면 장삼포로 535-57', 36.43358, 126.35595, '안면도 서쪽의 한적한 해변이에요. 방파제 위 솔숲(솔섬)이 물에 비치는 풍경과 별을 함께 찍는 곳으로 알려져 있어요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '솔섬 방파제 앞', '해변 주차장에 대고 남쪽 끝의 솔숲 방파제로 걸어가요. 주차장과 화장실이 있지만 자리가 넉넉하지 않아요. 지도의 위치는 해변 입구쯤이에요.', '솔숲이 물에 비치는 모습은 밀물 때만 보여요. 가기 전에 물때를 확인하세요. 맨눈으로 은하수가 보이는지는 후기가 엇갈려요. 파도가 높은 날에는 물가에 가까이 가지 마세요.'),
    (15, '부여 성흥산성', '충청남도 부여군 임천면 성흥로97번길 167', 36.1961432, 126.9002635, '가림성이라고도 하는 백제 산성이에요. 성벽 위의 큰 느티나무(사랑나무)와 별을 함께 찍는 곳으로 유명해요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '사랑나무', '정상 아래 무료 주차 공간에 대고 계단과 오르막을 10~20분 걸어요. 올라가는 길이 좁고 가팔라요.', '해가 지면 계단이 금방 어두워져요. 랜턴을 챙기세요. 성벽 둘레에 조명이 있고 남쪽 아래 시가지 불빛이 그대로 보여서, 별은 기대보다 덜 보여요.'),
    (16, '대전 시민천문대', '대전광역시 유성구 과학로 213-48', 36.38004, 127.35376, '대덕연구단지 안의 도심 시민천문대예요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '오후 2시부터 밤 10시까지 무료로 자유롭게 관람해요. 월요일은 쉬어요.', NULL),
    (17, '청양 칠갑산천문대', '충청남도 청양군 정산면 한티고개길 178-46', 36.43375, 126.88987, '칠갑산의 군립 천문대예요.', NULL, true, true, true, 'FACILITY', 'ACTIVE', NULL, '홈페이지에서 예약해야 들어가요. 월요일은 쉬어요. 주차장에서 5분 걸어요.', NULL),
    (18, '영양 반딧불이생태공원', '경상북도 영양군 수비면 반딧불이로 129', 36.83047, 129.26906, '2015년 아시아에서 처음 국제밤하늘보호공원으로 지정된 곳이에요. 밤에 조명을 켜지 않아 은하수가 잘 보여요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '반딧불이천문대 앞 주차장', '공원은 늘 열려 있고 무료예요. 천문대 앞 주차장에 대요. 천문대 프로그램은 따로 예약해요.', '손전등과 휴대폰 화면을 줄여 주세요. 여름에는 사람이 많아요.'),
    (19, '합천 황매산', '경상남도 합천군 가회면 둔내리 산 219', 35.48649, 127.9843, '해발 800m대까지 차로 오르는 너른 평원이에요. 합천군이 은하수 명소로 알리는 곳이에요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '황매산군립공원 주차장', '군립공원은 24시간 들어갈 수 있어요. 주차는 유료이고 직원이 없으면 무인 정산기를 써요.', '정상 주차장의 유도등이 밤 10시까지 켜져 있어요. 철쭉제 기간에는 길이 통제돼요.'),
    (20, '영천 보현산', '경상북도 영천시 화북면 정각리 산 6-3', 36.16667, 128.98058, '정상에 한국천문연구원 천문대가 있는 산이에요. 천문대 옆 정상 주차장이 은하수 촬영지로 쓰여요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '정상 주차장', '정상 주차장까지 차로 올라가요. 무료예요. 천문대 전시관은 낮에만 열어요.', '공식 안내는 일몰 뒤 차량 출입을 막는다고 적고, 도로에 출입금지 표지가 있어요. 겨울이 아니면 실제로 막지는 않는다는 글이 있지만, 막혀 있으면 돌아오세요. 연구 시설 옆이라 전조등과 불빛을 꺼 주세요. 차박은 안 돼요.'),
    (21, '경주 풍력발전단지', '경상북도 경주시 문무대왕면 불국로 1056-185', 35.75048, 129.36411, '토함산 자락 능선의 풍력발전단지예요. 전망대 경풍루가 있어요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '경풍루 주차장', '늘 열려 있고 무료예요. 주차장에 40~50대가 서요. 화장실 같은 시설은 적어요.', '별 보는 곳으로 소개되지만 동호인의 관측 후기는 확인하지 못했어요.'),
    (22, '의령 한우산', '경상남도 의령군 궁류면 벽계리 (한우산 정상부)', 35.39736, 128.19308, '정상 가까이까지 차로 오르는 산이에요. 사방이 트이고 빛 공해가 적어 사진가들에게 알려져 있어요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '한우산 생태숲 주차장', '평일에는 정상부 주차장까지 차로 가요. 2026년 8월에 관측 시설과 숙소가 있는 한우산 별천지가 문을 열었어요.', '주말과 공휴일에는 쇠목재부터 차량이 통제될 수 있어요. 그때는 30분쯤 걸어요. 별천지가 들어선 뒤로 밤에 주차장을 누구나 쓸 수 있는지는 확인하지 못했어요.'),
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
    (67, '제주 마방목지', '제주특별자치도 제주시 516로 (용강동)', 33.42704, 126.60387, '5·16도로 옆의 너른 목장 초지예요. 밤에는 인적이 끊겨 별 보기 좋은 곳으로 소개돼요.', NULL, true, true, false, 'OPEN_SITE', 'ACTIVE', '마방목지 주차장', '도로변 주차장에 대요.', '소개한 자료가 2018년 것이라 지금의 밤 주차 여건은 확인하지 못했어요. 초지 안으로는 들어가지 마세요.'),
    (68, '남원 정령치휴게소', '전북특별자치도 남원시 산내면 정령치로 1523', 35.3637952, 127.5219979, '지리산 정령치 고갯마루(해발 1,172m)의 휴게소예요. 주차장 옆 언덕이 남원 시내 쪽 불빛을 가려 주고, 맨눈으로 은하수를 봤다는 후기가 있어요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '정령치휴게소 주차장', '고기삼거리나 달궁삼거리에서 굽은 산길 12km를 올라가요. 국립공원 주차장이라 주차 요금이 있어요. 밤에도 요금을 받는지는 확인하지 못했어요.', '겨울에는 길이 막혀요. 예년에는 12월 초부터 3월 중순이나 말까지였고, 2026년에는 11월 하순부터 눈으로 통행이 제한됐어요. 밤에는 탐방로에 들어갈 수 없고 취사와 야영은 금지예요. 휴게소 뒤 언덕에 오르면 남원 시내 불빛이 정면으로 보여요.'),
    (69, '영광 내산서원', '전라남도 영광군 불갑면 강항로 101', 35.2131756, 126.5174504, '불갑산 기슭의 서원이에요. 서원 앞 주차장은 호남에서 별 보는 사람들이 오래 찾아온 곳으로 여러 기사에 나와요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '내산서원 주차장', '서원 앞 주차장에 대요. 화장실이 있어요. 좌표는 서원 건물의 것이고 주차장은 그 앞이에요.', '서원은 오전 9시부터 오후 6시까지 열어요. 밤에 주차장이 열려 있는지는 공식 안내로 확인하지 못했어요. 주차장 바닥에 누워 있던 사람이 들어오는 차에 다친 일이 있으니 차가 다니는 자리에는 눕지 마세요.'),
    (70, '무주 적상산 전망대', '전북특별자치도 무주군 적상면 북창리 산119-8', 35.956041, 127.698515, '적상호 옆 해발 850m쯤의 전망대예요. 주변에 인공 조명이 거의 없어서 차를 세우고 별을 보는 사람들이 있다고 소개돼요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '적상산 전망대 주차장', '머루와인동굴 쪽에서 굽은 산길을 7km 넘게 올라가요. 전망대 옆에 주차장과 화장실이 있고 무료예요.', '겨울에는 길을 막는 해가 있어요(2015년에는 11월 26일부터 3월 31일까지). 한국관광공사 자료에는 일몰 전까지 연다고 적혀 있어서, 밤에 길이 열려 있는지는 가기 전에 확인하세요. 전망대 탑은 여는 시간이 따로 있어요.'),
    (71, '서귀포 거린사슴전망대', '제주특별자치도 서귀포시 1100로 791', 33.3074176, 126.4545255, '1100도로 중턱의 전망대예요. 서귀포 시내 야경과 밤하늘의 별을 함께 보는 곳으로 제주 관광 포털이 소개해요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '거린사슴전망대 주차장', '전망대 옆 주차장에 대요. 무료이고 자리가 많지 않아요. 화장실이 있어요.', '남쪽은 서귀포 시내 불빛이라 그쪽 하늘은 밝아요. 1100도로는 굽고 어두워요. 안개가 자주 껴요.'),
    (72, '철원 노동당사', '강원특별자치도 철원군 철원읍 관전리 (금강산로)', 38.2551924, 127.2019235, '민통선 가까이에 남아 있는 옛 건물이에요. 주변에 불빛이 적어서 건물 뒤로 뜨는 별과 은하수를 찍는 곳으로 한국관광공사가 소개해요.', NULL, NULL, true, NULL, 'OPEN_SITE', 'ACTIVE', '노동당사 앞', '도로변의 야외 유적이에요. 차를 대는 자리는 확인하지 못했어요. 좌표는 건물의 것이에요.', '건물 조명이 저녁 8시부터 자정까지 켜져요(2021년 기준). 별은 자정 뒤에 보세요. 야간 행사가 있는 기간에는 붐벼요. 민통선 바로 앞이니 정해진 길 밖으로 나가지 마세요.'),
    (73, '강화 고인돌공원', '인천광역시 강화군 하점면 부근리 330-2', 37.7739476, 126.437158, '부근리 고인돌이 있는 너른 잔디 공원이에요. 수도권에서 가깝고 주변에 높은 건물이 없어서 별을 보러 오는 사람이 많아요. 하늘이 아주 어둡지는 않아요.', NULL, true, true, NULL, 'OPEN_SITE', 'ACTIVE', '고인돌공원 주차장', '공원 주차장에 대고 고인돌까지 5분쯤 걸어요. 공원은 늘 열려 있어요. 좌표는 고인돌의 것이에요.', '서울과 김포 쪽 하늘은 밝고 주변 불빛도 보여요. 고인돌 조명이 꺼지는 시각은 확인하지 못했어요. 맑은 날에는 사람이 많아요.'),
    (74, '합천 대암산', '경상남도 합천군 초계면 원당리 산42', 35.5338667, 128.2173528, '합천 운석충돌구를 내려다보는 해발 591m 정상이에요. 패러글라이딩 활공장이 있어 하늘이 넓게 트여 있고, 한국관광공사가 한산한 은하수 촬영지로 소개해요.', NULL, true, true, NULL, 'OPEN_SITE', 'ACTIVE', '대암산 정상 활공장', '초계면 원당마을 쪽에서 콘크리트 임도로 올라가요. 정상 아래 운석충돌구 주차장에 대거나 활공장까지 올라가요. 좌표는 정상의 것이에요.', '장지마을 쪽 길은 좁고 가팔라서 사륜구동이 아니면 어려워요. 활공장 가장자리는 낭떠러지예요. 밤에 길이 열려 있는지를 적은 안내는 찾지 못했어요.'),
    (75, '군위 화산마을', '대구광역시 군위군 삼국유사면 화북4리', 36.105795, 128.7785733, '해발 700m 고랭지 마을이에요. 풍차전망대에서 보는 일몰과 은하수 사진으로 알려져 있어요.', NULL, true, true, NULL, 'OPEN_SITE', 'ACTIVE', '화산마을 풍차전망대', '국도 28호선에서 굽잇길 7.6km를 올라가요. 화산마을 2주차장에 대고 풍차전망대까지 20분쯤 걸어요.', '주민이 사는 마을이에요. 밤에는 전조등과 소리를 줄여 주세요. 마을 안길은 좁은 일방통행이에요. 겨울에는 길이 얼 수 있어요.'),
    (76, '의성 달빛공원', '경상북도 의성군 사곡면 양지리 산64-2', 36.3107461, 128.8025264, '의성군이 별 보는 곳으로 가꾼 언덕 위 공원이에요. 주변에 빛 공해가 적고 공원도 조명을 줄여 두어서 별이 잘 보인다고 여러 기사가 소개해요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '달빛공원 광장', '공원 광장에 차를 대요. 전망대와 평상, 별자리 안내판이 있고 화장실이 있어요.', '노지 캠핑을 하는 사람이 많아서 주말에는 랜턴과 차량 불빛이 있을 수 있어요.'),
    (77, '영양 맹동산 풍력발전단지', '경상북도 영양군 석보면 요원리 산31-101', 36.5716667, 129.2423944, '해발 800m 안팎의 능선에 풍력발전기가 늘어선 곳이에요. 대구와 경북에서 맨눈으로 은하수를 볼 수 있는 곳으로 꼽혀요.', NULL, NULL, true, NULL, 'OPEN_SITE', 'ACTIVE', '능선 임도변 공터', '정상부까지 포장된 임도로 올라가요. 정식 주차장은 없고 길가 공터에 세워요. 좌표는 맹동산 정상의 것이라 대략의 위치예요.', '풍력발전기를 관리하는 길이에요. 밤이나 겨울에 막는지는 확인하지 못했어요. 발전기 소리가 나요.'),
    (78, '춘천 건봉령 승호대', '강원특별자치도 춘천시 북산면 부귀리 산59-8', 37.9555, 127.8622, '소양호를 내려다보는 고갯마루예요. 근방에 불빛이 없어 은하수를 보는 곳으로 알려져 있어요.', NULL, false, true, false, 'OPEN_SITE', 'ACTIVE', '승호대 고갯마루', '시골길과 산길을 10km쯤 올라가요. 주차장이 없어서 길가에 세워요. 늘 열려 있어요.', '차를 댈 자리가 몇 대뿐이라 맑은 날에는 자리가 없을 수 있어요. 화장실이 없어요. 좁은 산길이라 밤 운전에 주의하세요.'),
    (79, '부안 솔섬', '전북특별자치도 부안군 변산면 도청리 313-1', 35.5923181, 126.4887568, '전북학생해양수련원 앞 바다에 떠 있는 작은 섬이에요. 낙조로 유명하고, 달이 없는 맑은 밤에는 은하수도 찍는 곳이에요.', NULL, NULL, true, NULL, 'OPEN_SITE', 'ACTIVE', '수련원 앞 해변', '수련원에 대고 내려간다는 글과 근처 공영주차장에 댄다는 글이 있어요. 가기 전에 주차할 곳을 확인하세요.', '수련원은 교육 시설이에요. 밤에 그 주차장을 써도 되는지는 확인하지 못했어요.'),
    (80, '철원 백마고지 전적지', '강원특별자치도 철원군 철원읍 대마1길 72', 38.2689856, 127.1655698, '민통선 가까이의 전적지예요. 불빛이 적어서 백마상 뒤로 뜨는 은하수를 찍는 곳으로 소개돼요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '전적지 주차장', '전적지 주차장에 대요. 무료이고 화장실이 있어요.', '안내된 이용 시간은 오전 9시부터 오후 4시예요. 밤에 주차장이 열려 있다는 근거는 2020년 기사 하나뿐이니 가기 전에 확인하세요. 들어갈 때는 전조등을 줄여 주세요.'),
    (81, '서귀포 가시리 유채꽃프라자', '제주특별자치도 서귀포시 표선면 녹산로 464-65', 33.3937315, 126.7317658, '가시리 풍력단지 한가운데의 중산간 시설이에요. 맑은 날에는 은하수가 선명하게 보인다고 소개돼요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '유채꽃프라자 주차장', '프라자 주차장에 대요. 무료예요.', '시설은 오후 5시 30분에 닫아요. 그 뒤에 진입로와 주차장이 열려 있는지는 확인하지 못했어요. 여름에는 바다 쪽에 어선 불빛이 보여요.'),
    (82, '밀양 얼음골 주차장', '경상남도 밀양시 산내면 산내로 1647', 35.5748544, 128.9856375, '밀양아리랑우주천문대가 야외 관측 행사에 쓴 주차장이에요. 밀양에서 별 보는 사람들이 찾는 곳으로 소개돼요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '얼음골 주차장', '주차장에 24시간 여는 화장실이 있어요. 좌표는 얼음골 안내판의 것이라 대략의 위치예요.', '깊은 계곡이라 하늘이 좁을 수 있어요. 소개한 자료가 2021년 것뿐이에요.'),
    (83, '영암 활성산 풍력발전단지', '전라남도 영암군 금정면 활성산길 219-1', 34.8095, 126.7615, '산 위 풍력발전기 사이로 별을 보는 곳이에요. 전남도와 한국관광공사가 별 보는 곳으로 소개해요.', NULL, NULL, true, false, 'OPEN_SITE', 'ACTIVE', '발전단지 아래 공터', '좁고 가파른 임도를 올라가요. 이름 붙은 주차장은 없고 들어갈 수 있는 구역의 빈터에 세워요. 좌표는 활성산 정상의 것이라 대략의 위치예요.', '정상의 발전단지는 사유지라 들어갈 수 없어요. 화장실과 편의시설이 없어요. 밤에 임도를 오르기가 쉽지 않아요.'),
    (84, '장흥 소등섬', '전라남도 장흥군 용산면 상발리 산225', 34.6046207, 127.0047356, '남포마을 앞바다의 작은 무인도예요. 일출과 별을 함께 보는 곳으로 전남도가 소개해요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '남포마을 앞 주차장', '마을 앞 주차장에 대요. 화장실이 있고 늘 열려 있어요.', '섬은 썰물 때만 걸어 들어갈 수 있어요. 밤에는 바닷길에 들어가지 마세요. 마을 불빛이 있어요.'),
    (85, '태백 스포츠파크', '강원특별자치도 태백시 백두대간로 179', 37.2000078, 128.9748627, '해발 812m의 운동 공원이에요. 시내와 떨어져 있어서 태백시와 한국관광공사가 가기 쉬운 은하수 관측지로 소개해요.', NULL, NULL, true, NULL, 'OPEN_SITE', 'ACTIVE', '스포츠파크', '좌표는 경기장의 것이에요. 밤에 주차장이 열려 있는지는 확인하지 못했어요.', '경기장 조명이 켜지는 날이 있을 수 있어요. 소개한 자료가 2022년 것이에요.'),
    (86, '원주 거돈사지', '강원특별자치도 원주시 부론면 정산리 188', 37.1930719, 127.7920893, '너른 절터에 삼층석탑이 남아 있는 곳이에요. 수도권에서 1시간 남짓이라 석탑과 은하수를 찍으러 오는 사람들이 있어요.', NULL, NULL, true, NULL, 'OPEN_SITE', 'ACTIVE', '절터 앞 길가', '절터 앞 길가에 세운다고 출사 글이 적어요.', '근거가 출사 글 하나예요. 밤 출입과 조명은 확인하지 못했어요.'),
    (87, '고성 상족암군립공원', '경상남도 고성군 하이면 덕명리', 34.9073092, 128.1477521, '공룡 발자국 화석이 있는 해안 공원이에요. 상족암 동굴과 은하수를 함께 찍는 곳으로 알려져 있어요.', NULL, NULL, true, NULL, 'OPEN_SITE', 'ACTIVE', '군립공원 주차장', '좌표는 공원의 것이라 대략의 위치예요. 주차와 밤 출입은 확인하지 못했어요.', '동굴은 썰물 때만 갈 수 있어요. 밤에는 물가와 동굴에 들어가지 말고 주차장 쪽에서 보세요.'),
    (88, '남해 가천 다랭이마을', '경상남도 남해군 남면 홍현리', 34.7253469, 127.8934013, '바다로 흘러내리는 계단식 논 마을이에요. 유채꽃과 은하수를 함께 담는 사진으로 알려져 있어요.', NULL, NULL, true, NULL, 'OPEN_SITE', 'ACTIVE', '다랭이마을 전망대', '도로변 전망대에서 마을과 바다를 내려다봐요. 주차는 확인하지 못했어요.', '주민이 사는 마을이라 밤에는 조용히 해 주세요. 마을과 펜션 불빛이 있어요.'),
    (89, '함양 오도재 지리산조망공원', '경상남도 함양군 마천면 지리산가는길 534', 35.4414, 127.6933, '오도재 고갯마루의 조망공원이에요. 지리산 능선이 한눈에 보이고, 은하수 구경도 좋다고 소개돼요.', NULL, true, true, true, 'OPEN_SITE', 'ACTIVE', '지리산조망공원 주차장', '오도재 위쪽에 주차장과 화장실이 있어요. 무료이고 늘 열려 있어요.', '별 보는 곳이라는 근거는 기사 한 문장 정도예요. 고갯길이 굽어 있어요.')
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

-- 지형 너머로 직접 보이는 먼 불빛의 지수. scripts/lightpollution/terrain.py가 위 좌표와 고도 자료로 계산했다
-- (2024년 야간 불빛, 2026-11-26 계산). 좌표를 고치면 다시 계산해서 여기를 고친다. 값이 클수록 아래쪽 마을이나
-- 도시의 불빛이 많이 보이고, 0.2 아래면 거의 보이지 않는다.
-- 밤에 가는 관측지에만 넣었다. 좌표가 대략의 위치인 8번, 10번, 14번은 몇십 m에 따라 값이 크게 달라져서 비웠다.
-- 69, 72, 73, 74번은 건물이나 유적, 정상의 좌표에서 계산했다. 보는 자리가 거기서 100m 안쪽이라 값을 넣었다.
-- 좌표가 대략인 77, 82, 83, 87번도 비웠다.
-- 13번과 15번의 description과 caution에 적은 불빛 이야기는 다녀온 사람이 본 것이고, 계산값과 같은 방향이다.
UPDATE observation_spot
SET lights_in_view = CASE id
        WHEN 1 THEN 0.18   -- 강릉 안반데기
        WHEN 2 THEN 0.02   -- 평창 육백마지기
        WHEN 4 THEN 0.39   -- 화천 조경철천문대
        WHEN 5 THEN 1.05   -- 정선 타임캡슐공원
        WHEN 9 THEN 0.37   -- 가평 화악터널 쌈지공원
        WHEN 12 THEN 0.00  -- 연천 당포성
        WHEN 13 THEN 0.16  -- 제천 덕주산성
        WHEN 15 THEN 2.36  -- 부여 성흥산성
        WHEN 18 THEN 0.03  -- 영양 반딧불이생태공원
        WHEN 19 THEN 0.31  -- 합천 황매산
        WHEN 20 THEN 0.45  -- 영천 보현산
        WHEN 21 THEN 1.37  -- 경주 풍력발전단지
        WHEN 22 THEN 0.05  -- 의령 한우산
        WHEN 28 THEN 0.02  -- 서귀포 1100고지
        WHEN 30 THEN 1.09  -- 제주 새별오름
        WHEN 40 THEN 0.52  -- 함백산 만항재
        WHEN 53 THEN 0.74  -- 거창 감악산
        WHEN 64 THEN 0.21  -- 제주 용눈이오름
        WHEN 65 THEN 1.77  -- 합천 오도산
        WHEN 66 THEN 0.02  -- 정선 문치재
        WHEN 67 THEN 0.20  -- 제주 마방목지
        WHEN 68 THEN 0.08  -- 남원 정령치휴게소
        WHEN 69 THEN 0.33  -- 영광 내산서원
        WHEN 70 THEN 0.20  -- 무주 적상산 전망대
        WHEN 71 THEN 4.46  -- 서귀포 거린사슴전망대
        WHEN 72 THEN 1.66  -- 철원 노동당사
        WHEN 73 THEN 6.55  -- 강화 고인돌공원
        WHEN 74 THEN 1.82  -- 합천 대암산
        WHEN 75 THEN 1.36  -- 군위 화산마을
        WHEN 76 THEN 0.70  -- 의성 달빛공원
        WHEN 78 THEN 0.01  -- 춘천 건봉령 승호대
        WHEN 79 THEN 1.75  -- 부안 솔섬
        WHEN 80 THEN 3.08  -- 철원 백마고지 전적지
        WHEN 81 THEN 0.53  -- 서귀포 가시리 유채꽃프라자
        WHEN 84 THEN 0.93  -- 장흥 소등섬
        WHEN 85 THEN 0.83  -- 태백 스포츠파크
        WHEN 86 THEN 0.08  -- 원주 거돈사지
        WHEN 88 THEN 0.87  -- 남해 가천 다랭이마을
        WHEN 89 THEN 0.51  -- 함양 오도재 지리산조망공원
        ELSE NULL END;

-- 길이 막히거나 주차장을 닫아 갈 수 없는 기간. 이 기간의 밤에는 추천 후보에서 뺀다. 겨울마다 날짜가 달라지므로
-- 해마다 여기를 고친다. 2번은 평창군이 알린 날짜다. 68번은 올겨울의 통제 고시를 찾지 못해, 눈으로 통행이 제한된
-- 2026년 11월 하순부터 예년에 길이 열린 3월 말까지로 잡았다. 70번도 올겨울 날짜를 찾지 못해 2015년의 통제 기간에
-- 맞춰 잡았다. 통제가 잦지만 기간이 정해져 있지 않은 곳(미시령 옛길, 만항재)은 caution에 글로만 적는다.
UPDATE observation_spot
SET closed_from = CASE id
        WHEN 2 THEN '2026-11-30'   -- 평창 육백마지기
        WHEN 68 THEN '2026-11-24'  -- 남원 정령치휴게소
        WHEN 70 THEN '2026-12-01'  -- 무주 적상산 전망대
        ELSE NULL END,
    closed_until = CASE id
        WHEN 2 THEN '2027-04-01'
        WHEN 68 THEN '2027-03-31'
        WHEN 70 THEN '2027-03-31'
        ELSE NULL END;

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
    (67, 0, '낭만이 빛나는 밤에', 'https://twig24.com/news/lifestyle/trip/2018/06/28/20180628027006', 'https://img.seoul.co.kr//img/upload/2018/06/27/SSI_20180627162429.jpg'),
    (68, 0, '지리산 정령치에서 은하수를 찍어보았습니다', 'https://community.samsung.com/kr/conversations/%EA%B0%A4%EB%9F%AC%EB%A6%AC/%EC%A7%80%EB%A6%AC%EC%82%B0-%EC%A0%95%EB%A0%B9%EC%B9%98%EC%97%90%EC%84%9C-%EC%9D%80%ED%95%98%EC%88%98%EB%A5%BC-%EC%B0%8D%EC%96%B4%EB%B3%B4%EC%95%98%EC%8A%B5%EB%8B%88%EB%8B%A4/r1_19069760', NULL),
    (68, 1, '지리산 정령치구간 차량 통행 전면 금지, 내년 3월 19일까지 (2020년 겨울)', 'http://news.tf.co.kr/read/national/1829484.htm', 'http://img.tf.co.kr/article/home/2020/12/07/202023081607327513.jpg'),
    (68, 2, '국립공원공단: 입산시간지정제', 'https://www.knps.or.kr/portal/main/contents.do?menuNo=8000198', NULL),
    (69, 0, '별똥별 쏟아진다, 전국 별 관측 명소 총정리', 'https://www.seoul.co.kr/news/society/science-news/2016/08/12/20160812500172', 'https://img.seoul.co.kr//img/upload/2016/08/12/SSI_20160812135419_O2.jpg'),
    (69, 1, '내산서원 이용 안내', 'https://www.ktriptips.com/kor/tourspot/126415', 'https://tong.visitkorea.or.kr/cms/resource/65/3379065_image2_1.JPG'),
    (70, 0, '잊지 못할 별의 향연을 만나볼 수 있는 국내 숨겨진 별빛 명소 BEST 4', 'https://www.tourtoctoc.com/news/articleView.html?idxno=8802', 'https://cdn.tourtoctoc.com/news/thumbnail/202504/8802_50769_4036_v150.jpg'),
    (70, 1, '차로 해발 850m 전망대까지 올라간다니', 'https://www.ardentnews.co.kr/news/articleView.html?idxno=21381', 'https://cdn.ardentnews.co.kr/news/thumbnail/202611/21381_90051_1043_v150.jpg'),
    (71, 0, '비짓제주: 아이와 떠나는 생태여행, 별 보러 가자', 'https://www.visitjeju.net/kr/detail/view?contentsid=CNTS_200000000013886', NULL),
    (71, 1, '제주도 밤에 가볼만한 곳 추천 BEST 9', 'https://v.daum.net/v/fZHd5riH2w', 'https://img1.daumcdn.net/thumb/S800x400/?fname=https://t1.daumcdn.net/news/202309/21/traveltoktok/20230921194647095vdmn.jpg'),
    (72, 0, '대한민국 구석구석: 그때는 전쟁의 공간 지금은 평화의 공간, 철원 노동당사', 'https://korean.visitkorea.or.kr/detail/rem_detail.do?cotid=2cc91cab-abc0-4b40-a55b-553f44dc5499', 'https://cdn.visitkorea.or.kr/img/call?cmd=VIEW&id=c80256d1-9f55-4a9f-ad3f-20beeb3cd268'),
    (72, 1, '별바라기 때 만났다, 은하수가 쏟아지는 그곳', 'https://www.hankookilbo.com/news/article/A2022051709180000820', 'https://newsimg.hankookilbo.com/2022/05/17/28a70b26-96a5-4a52-a2f2-4113116b386c.jpg'),
    (73, 0, '출사정보: 강화도 고인돌 공원 은하수', 'http://www.soonmo.com/index.php?mid=board_uZCu39&document_srl=16852', NULL),
    (73, 1, '인천경제자유구역청 저널의 인천 별 보기 명소 소개', 'https://www.ifez.go.kr/journal/now/view.do?jrnl_pst_sn=88', NULL),
    (74, 0, '대한민국 구석구석: 나만의 겨울왕국을 찾아서, 푸른하늘 은하수.zip', 'https://korean.visitkorea.or.kr/detail/rem_detail.do?cotid=02392a8b-dfe8-4cf6-94a8-59a0d81d75cc', 'https://cdn.visitkorea.or.kr/img/call?cmd=VIEW&id=f372fce1-1e0d-4c4c-b71b-7eecfcf83597'),
    (74, 1, '합천 운석충돌구 환 종주: 큰고개~대암산~박골재', 'https://v.daum.net/v/20251029191423173', 'https://img1.daumcdn.net/thumb/S1200x630/?fname=https://t1.daumcdn.net/news/202510/29/kookje/20251029191424325aihn.jpg'),
    (75, 0, '하늘 아래 첫 동네 군위 화산마을, 사방을 둘러봐도 절경', 'https://www.imaeil.com/page/view/2025122401384118235', 'https://www.imaeil.com/photos/2025/12/24/2025122413091723233_l.jpg'),
    (75, 1, '군위 킥 삼대장, 구불구불 고갯길 끝 구름바다', 'https://www.khan.co.kr/article/202411230900025', 'https://img.khan.co.kr/news/2024/11/23/l_2024112201000646100060181.jpg'),
    (76, 0, '별 사진 찍기 최고, 의성달빛공원 재정비 완료', 'http://www.asiatoday.co.kr/view.php?key=20250105010002079', 'https://img.asiatoday.co.kr/file/2025y/01m/05d/2025010501000345900020791.jpg'),
    (76, 1, '의성 달빛공원 수놓은 은하수', 'http://v.daum.net/v/20250611145517060', 'https://img1.daumcdn.net/thumb/S1200x630/?fname=https://t1.daumcdn.net/news/202506/11/551746-KJTfZ0q/20250611145518775sjba.jpg'),
    (77, 0, '지금 가기 딱 좋은 청정 1번지 영양: 맹동산과 삼의계곡', 'https://www.yeongnam.com/web/view.php?key=20200901010000168', 'https://www.yeongnam.com/mnt/file/202009/2020090101000066500001681.jpg?20200903074235'),
    (77, 1, '스위스 못지않은 풍경, 무료 입장 영양 힐링 스팟 4', 'https://brunch.co.kr/@qrssa/5158', 'https://img1.kakaocdn.net/thumb/R1280x0.fjpg/?fname=http://t1.kakaocdn.net/brunch/service/user/aKRc/image/QO9NgjmY4BIuermSwKhHSidjJrM.jpg'),
    (78, 0, '건봉령 승호대 이용 안내', 'https://infotravelog.com/places/2288', NULL),
    (78, 1, '출사정보: 춘천 건봉령 승호대 은하수', 'http://www.soonmo.com/index.php?document_srl=15199&listStyle=webzine&mid=board_uZCu39&page=16', NULL),
    (79, 0, '국내 겨울 여행은 여기로, 부안 변산반도의 맛과 멋', 'https://www.vegemil.co.kr/webzine/Q1_2025/sub14', 'https://image.edaymall.com/images/dcf/webzine/Q1_2025/14_sns.png'),
    (79, 1, '올해 가기 전 소원 빌러 가야 하는 국내 은하수 명소', 'https://brunch.co.kr/@6a51c9907a4f468/100', 'https://img1.kakaocdn.net/thumb/R1280x0.fjpg/?fname=http://t1.kakaocdn.net/brunch/service/user/cgRg/image/qMc9uQ2jJPvWKKQYicUdSoK46ME.jpg'),
    (80, 0, '별구경, 나만 알고 싶은 명당 장소 BEST4', 'https://www.harpersbazaar.co.kr/article/49710', 'https://www.harpersbazaar.co.kr/resources_old/online/org_thumnail_image/62036dc5-730f-40ae-8fb3-07a81cea9656.jpg'),
    (80, 1, '백마고지 위령비와 기념관 이용 안내', 'https://www.ktourmap.com/spotDetails.jsp?contentId=731587', 'https://tong.visitkorea.or.kr/cms/resource/89/4133289_image2_1.jpg'),
    (81, 0, '은하수를 맨눈으로 감상할 수 있는 명소 BEST5', 'https://www.allurekorea.com/?p=202184', 'https://www.allurekorea.com/wp_data/allure/2023/04/style_643fa135e7781.jpeg'),
    (81, 1, '유채꽃프라자 이용 안내', 'https://ontrip.kr/travel-guides/details/2755049', 'https://tong.visitkorea.or.kr/cms/resource/31/4064231_image2_1.jpg'),
    (82, 0, '밀양아리랑우주천문대, 차박 별 관측 프로그램 운영', 'https://www.busan.com/view/busan/view.php?code=2021040817213924996', 'https://www.busan.com/nas/wcms/wcms_data/photos/2021/04/08/2021040817221112563_l.jpg'),
    (83, 0, '대한민국 구석구석: 활성산 풍력발전단지', 'https://korean.visitkorea.or.kr/detail/ms_detail.do?cotid=3f0298aa-96bb-47c6-aa3a-1cc89764ce5d', 'https://cdn.visitkorea.or.kr/img/call?cmd=VIEW&id=5a7882a5-1a1e-478f-a615-2c85254bec34'),
    (83, 1, '차박 마니아의 비밀 여행지, 가을 드라이브로 즐겨볼까', 'https://brunch.co.kr/@d54dc50e53704c1/545', 'https://img1.kakaocdn.net/thumb/R1280x0.fjpg/?fname=http://t1.kakaocdn.net/brunch/service/user/hdzi/image/QOz2DwNa_j2KSBnOK-oHbnGCckg.jpg'),
    (84, 0, '소등섬 이용 안내와 후기', 'https://m.tripinfo.co.kr/info.html?content_type_id=12&content_id=2514005', 'https://thumb2.tripinfo.co.kr/thumb.php?url=http://tong.visitkorea.or.kr/cms/resource/55/2618255_image2_1.jpg?OPT=proxy'),
    (84, 1, '소원 비는 노을 명소, 장흥 소등섬', 'https://www.balpumnews.com/travel/sodeung-island', 'https://www.balpumnews.com/wp-content/uploads/2025/08/Sodeung-Island.jpg'),
    (85, 0, '대한민국 구석구석: 청정고원 태백으로 떠나는 은하수 여행', 'https://korean.visitkorea.or.kr/detail/rem_detail.do?cotid=9185d4da-c82d-4fe5-b6ce-a9aedbe6d631', 'https://cdn.visitkorea.or.kr/img/call?cmd=VIEW&id=dc92f1fe-c76f-42d3-a1fc-c8ec559c8982'),
    (85, 1, '열대야 없고 별 볼 일 있는 밤, 태백에서 은하수 여권 찍어볼까', 'https://www.khan.co.kr/article/202206212211025', 'https://img.khan.co.kr/news/2022/06/21/l_2022062201002342900226911.jpg'),
    (86, 0, '출사정보: 원주 거돈사지 은하수', 'http://www.soonmo.com/index.php?mid=board_uZCu39&document_srl=19387', NULL),
    (87, 0, '지금 고성에 가면 볼 수 있는 은하수', 'https://www.insight.co.kr/news/228271', 'https://img.insight.co.kr/static/2019/05/20/1200/ajn583rxjrvna7bp4ad1.jpg'),
    (87, 1, '별 볼 일 없는 도시를 떠나자, 은하수 선명한 여름 가기 전에', 'http://v.daum.net/v/20190810030224978', 'https://img1.daumcdn.net/thumb/S1200x630/?fname=https://t1.daumcdn.net/news/202010/28/chosun/20201028134120332vpzm.jpg'),
    (88, 0, '남해 다랭이 마을의 밤마실, 유채꽃과 은하수', 'http://v.daum.net/v/20220406105420539', 'https://img1.daumcdn.net/thumb/S1200x630/?fname=https://t1.daumcdn.net/news/202204/06/pressian/20220406105421674ssgo.jpg'),
    (89, 0, '함양 지리산조망공원과 오도재', 'https://tripgend.co.kr/domestic/20435/jirisan-viewpark-hamyang/', 'https://tripgend.co.kr/wp-content/uploads/2025/09/cw01296_img02.jpg');

-- 명소 안에서 서서 보는 자리들. 산이나 고원 하나에 자리가 여럿인 곳에 적는다. 첫 자리가 차를 대는 곳이고,
-- 나머지는 거기서 차로 조금 가거나 30분 안쪽으로 걸어가는 곳이다. 좌표는 아는 자리에만 넣는다.
-- 이 표도 이 파일이 원본이라 매번 비우고 다시 넣는다.
DELETE FROM observation_spot_point;
INSERT INTO observation_spot_point (spot_id, position, name, access, latitude, longitude)
VALUES
    (1, 0, '멍에전망대 주차장', '차박하는 사람들이 모이는 곳이에요. 전망대가 바로 옆이에요.', 37.626351, 128.739391),
    (1, 1, '멍에전망대', '주차장에서 돌계단으로 올라가요. 별과 은하수를 찍는 대표 자리예요.', 37.626169, 128.741501),
    (1, 2, '피덕령 주차장', '대기4리 마을회관 쪽이에요. 멍에전망대 주차장이 찼을 때 대요. 멍에전망대까지 직선으로 600m쯤이에요.', 37.623106, 128.736238),
    (1, 3, '일출전망대', '옥녀봉 쪽 언덕에 있는 또 하나의 전망대예요. 피덕령에서 풍력발전기 쪽으로 오르막을 올라가요. 걸리는 시간은 확인하지 못했어요.', NULL, NULL),
    (2, 0, '정상부 주차장', '1주차장과 2주차장이 있어요. 차 안에서 쉬면서 별을 볼 수 있어요.', 37.406267, 128.511653),
    (2, 1, '청옥산 전망대 정자', '주차장에서 직선으로 500m쯤이에요. 데크길과 풍력발전기 쪽 자리들도 걸어서 10~15분 안에 있어요.', 37.410293, 128.508409),
    (15, 0, '성흥산성 주차장', '정상 아래의 무료 주차 공간이에요. 여기서부터 걸어요.', NULL, NULL),
    (15, 1, '사랑나무', '주차장에서 계단과 오르막을 10~20분 걸어요. 남쪽이 트여 있어서 아래 시가지 불빛도 그쪽으로 보여요.', 36.1961432, 126.9002635),
    (19, 0, '군립공원 정상 주차장', '해발 850m까지 차로 올라가요. 별쿵캠핑장(옛 황매산오토캠핑장)이 바로 아래라 사실상 같은 자리예요. 주차장에서 바로 볼 수 있고 유도등은 밤 10시에 꺼져요.', 35.48649, 127.9843),
    (19, 1, '별빛언덕', '주차장이나 캠핑장에서 억새 평원 쪽으로 20분쯤 걸어요. 등산이 아니라 언덕길을 걷는 정도예요. 밤에는 랜턴을 챙기세요.', NULL, NULL),
    (20, 0, '천문대 아래 주차장', '52대를 댈 수 있어요. 별 사진은 대부분 여기서 찍어요.', 36.166686, 128.98058),
    (20, 1, '천수누림길 데크', '주차장에서 시작해 정상까지 1km 이어지는 나무 데크길이에요. 걸리는 시간은 확인하지 못했어요.', NULL, NULL),
    (22, 0, '정상부 주차장', '옆에 화장실이 있어요. 2026년 8월에 문을 연 한우산 별천지가 이 주차장 자리에 있어요.', 35.397405, 128.193081),
    (22, 1, '한우정', '큰 공터에 정자가 있어요. 여기서 정상까지 데크로 20분이 안 걸려요.', NULL, NULL),
    (22, 2, '하늘전망대', '높이 16m 전망대예요. 홍의광장 주차장에서 걸어서 10분이면 정상에 닿아요.', 35.393872, 128.193339),
    (40, 0, '만항재쉼터', '고갯마루의 쉼터예요. 화장실이 있어요.', 37.148491, 128.899314),
    (40, 1, '함백산소공원 주차장', '쉼터에서 동쪽으로 250m쯤이에요. 무료이고 화장실이 있어요.', 37.148751, 128.902188),
    (40, 2, '오투전망대', '차로 가는 태백 쪽 은하수길의 자리예요. 팔각정이 있어 사람이 많지만 주차 시설이 없고 태백 시내 불빛이 들어와요.', NULL, NULL),
    (53, 0, '별바람언덕 주차장', '무료이고 화장실과 쉼터가 있어요.', 35.590979, 127.920203),
    (53, 1, '별바람언덕 전망대', '언덕 가장 높은 곳이에요. 밤에 외벽에 조명 영상을 틀어요. 꺼지는 시각은 확인하지 못했어요.', NULL, NULL),
    (53, 2, '무장애 나눔길 데크', '계단이 없는 데크길이에요. 갈림길에서 정상까지 12분쯤이고, 덕유산 쪽과 가야산 쪽을 보는 전망 데크가 있어요.', NULL, NULL),
    (74, 0, '운석충돌구 주차장', '정상 600m 아래의 안부에 있어요.', NULL, NULL),
    (74, 1, '정상 활공장', '주차장에서 콘크리트 임도로 이어져요. 사방이 트여 있고 가장자리는 낭떠러지예요.', 35.5338667, 128.2173528),
    (75, 0, '화산마을 2주차장', '승용차 300대를 댈 수 있어요. 전망대는 여기서 걸어가요.', NULL, NULL),
    (75, 1, '풍차전망대', '주차장에서 걸어서 20분쯤이에요. 풍차를 넣은 은하수 사진을 찍는 자리예요.', 36.105795, 128.7785733),
    (75, 2, '하늘전망대', '걸어서 15분쯤이에요. 마을에서 가장 높은 쪽이라 아래 불빛이 덜 보여요. 임도가 좁고 굽어 있어요.', 36.098702, 128.7681355);

-- 명소에서 별을 본 유튜브 영상. 가기 전에 어떤 곳인지 눈으로 보라고 고른 남의 영상이다.
-- scripts/spots/videos.tsv에 고른 목록이 있고, scripts/spots/videos.py가 유튜브에서 제목과 채널 이름을 받아 이 행들을 만든다.
-- 링크, 보는 자리와 같이 시작할 때마다 비우고 다시 채운다.
DELETE FROM observation_spot_video;
INSERT INTO observation_spot_video (spot_id, position, video_id, title, channel, short_form)
VALUES
    (1, 0, 'S8tjRoOsgKo', '은하수 보러 안반데기? 준비없이 가면 망합니다.#국내은하수#안반데기가는길', '사즐', TRUE),
    (1, 1, 'w2pu1mB7_HI', '[차박 캠핑] 별 보기 좋은 곳 은하수 맛집 안반데기 멍에전망대 | 스텔스차박 | 노지캠핑 | 가을캠핑', 'ch.캠핑', FALSE),
    (1, 2, 'YZdzkbZ7Xlk', '화사가 반한 강릉 #안반데기, #별구경 명소의 비밀', '잡학다식여행', TRUE),
    (1, 3, 'xEfv8BDCpgw', '4K) 안반데기 3월 은하수 타임랩스', 'Han', FALSE),
    (2, 0, 'y41pUPz28M8', '육백마지기 별과 은하수를 담다 | 차박캠핑 | 스텔스차박 | 은하수 | 밤하늘 | 차박성지', 'BONJOUR VIDEO', FALSE),
    (2, 1, 'vBeFH2YUsnA', '평창  육백마지기 은하수! 9월~10월이 골든타임 이예요! #평창육백마지기 #평창', 'Star of Arabia', TRUE),
    (2, 2, 'ipJCuH66T1k', '해발 1200m에서 스텔스차박 / 육백마지기 / 강원도 평창 / 별사진찍기좋은곳', '스라캠핑', FALSE),
    (2, 3, 'BUKZBirf-W4', '강원도에서 별과 은하수 사진 찍기 / 차박성지/백패킹/솔캠/청옥산/육백마지기/별사진/Camping/Cooking', '포컬랜서 Focal Lancer', FALSE),
    (4, 0, 'N94bkt_K8D8', '[지구여행]조경철 천문대 를 아시나요? 별보기 명당', '루카월드', TRUE),
    (4, 1, 'Hx-dodov_uY', '갤럭시 S21 울트라 별 사진 찍기! / 화천 조경철 천문대 별사진 촬영', '현대왕윤서준', FALSE),
    (4, 2, 'zCpT7jONYg4', '아름다운 별들이 쏟아지는 "조경철천문대"', '오늘은 어디로 가지? [오어가]', TRUE),
    (4, 3, 'kONrCgWC_IA', '고도 1100미터 조경철 천문대에 다녀왔습니다!!', '우주를 줄게 - 천체관측&캠핑', FALSE),
    (5, 0, 'aWAjeLTfCKw', '정선새비재  타임캡슐공원 은하수, 강원도 별 구경하기 좋은곳', '이원형', FALSE),
    (5, 1, 'Qt3JDms7Jf0', '정선 타임캡슐공원&새비재(8월 12일 페르세우스 유성호&은하수 여행 안내)', '희망일출', FALSE),
    (5, 2, 'oRzvJR5uWHA', '강원도 정선 타임캡슐공원 은하수 타임랩스 4K', '이청희', FALSE),
    (5, 3, 'Frn3hjGGLZ4', '정선 타임캡슐공원 새비재 은하수', '대적반', TRUE),
    (8, 0, '7vlSua_vIu8', '울산바위 촬영포인트 - 미시령옛길 포인트소개', 'Venti', FALSE),
    (8, 1, 'MfZKulNoYZo', '미시령옛길은 낮에는 울산바위의 웅장한 풍광을, 밤에는 은하수와 어우러진 환상적인 야경을! #설악산 #여행 #울산바위', '나홀로 여행 중', TRUE),
    (8, 2, 'ty9WFzGK-Z8', '미시령 옛길에서 본 울산바위 은하수', 'Camp Stella', FALSE),
    (8, 3, 'buuXRV-1X24', '속초 미시령옛길 울산바위 은하수 타임랩스 4K', 'Venti', FALSE),
    (9, 0, 'jDqz4Ul17Us', '가평 화악터널쌈지공원 | 화사가 별 구경한 곳 | 오후 6시 30분부터 이미 만차... | 별만보러오기 좋은 곳', '다코니', FALSE),
    (9, 1, 'vQjANmxHM6E', '화악터널 쌈지공원, ''가평 별빛정원''으로 새단장', '가평군', FALSE),
    (9, 2, '7SGR2xo_OwE', '차박] 진짜 별거없는 화악터널쌈지공원 / 가평 / 나혼자산다 화사 / 별맛집 / 스파크차박 / 스텔스차박 / 경차차박 / 미니멀차박', '행복한면정', FALSE),
    (9, 3, 'Ijh-hajufvc', '차타고 가평 화악터널/별빛정원 별구경⭐️', '이얍', TRUE),
    (10, 0, 'iPoM6NxapB0', '양평 벗고개 은하수촬영지 파헤치기', '청암 tv', FALSE),
    (10, 1, 'neo4vzTWx1Y', '경기도 별 명소 별 잘보이는 곳 양평 벗고개터널', '찰나의 향유', TRUE),
    (10, 2, 'xNBNRT8qUuE', '▫️은하수 출사 VLOG / 별보러 양평 벗고개 터널 / 사진 소모임', '오기 온앤온 Oogie On & On', FALSE),
    (10, 3, '_-ydHK6HLl0', '양평벗고개 은하수 타임랩스', '청암 tv', FALSE),
    (12, 0, '5rcnomCJRr0', '별이 쏟아지는 당포성⭐️| 서울 근교 별보기 좋은 곳🌌 | 별 볼때 꿀팁✅', '언니와동생', FALSE),
    (12, 1, 'cLgDfNRHefQ', '서울 근교 "인생 별 사진 명소" 연천 당포성! 달빛 0% 삭(朔)의 밤', '마운틴 골렘', TRUE),
    (12, 2, 'F43bhct6Zx4', '연천 별 보러가기 장소는. 당포성!', 'ToGoBro 토고형', FALSE),
    (12, 3, 'mtxdAY6Iuqk', '연천군의 별보기 명당 당포성 별빛축제', '연천군', TRUE),
    (13, 0, 'JQTfUhmJCUY', '3월~8월 충북 은하수 포인트(제천 덕주산성)', '사진찍는 남자', FALSE),
    (13, 1, 'fMsjdXZuJSw', '겨율에 덕주산성에서 혼자 은하수 촬영? 후덜덜…! | Milky Way in November?', '소소투데이', FALSE),
    (13, 2, 'pp_Sio21v8U', '덕주산성 은하수와 별궤적, 제천가볼만한곳', '이원형', FALSE),
    (13, 3, 'IqxJCqPJgIQ', '월악산 덕주산성 달 밝은 날 은하수 촬영 가능?|Woraksan Deokjusanseong | Milky Way Photography on a Bright Moon Night 🌕', '소소투데이', FALSE),
    (14, 0, 'ysMmE18MSRQ', '은하수 보러가야겠지? (운여해변 촬영기)', '시나포토', FALSE),
    (14, 1, 'bJtq42Q_fwg', '운여해변에서 만난 완벽할뻔 했던 은하수.', 'wonguy shin', TRUE),
    (14, 2, 'bRdoX9KrCjs', '[출사]겨울 안면도 운여해변 야간 타임랩스 | 겨울 은하수 촬영', '달콤한인삼(sweetinsam)', FALSE),
    (14, 3, 'GbEAOaEQ-vc', '운여해변 은하수 타임랩스 (feat. 야광충) 4k', 'Venti', FALSE),
    (15, 0, 'xhGT54ZLVs4', 'Vlog. 부여 별구경 나들이 ⭐️ 성흥산성 사랑나무 낮과 밤 💕🌳🌌', '트루트루 truex2', FALSE),
    (15, 1, 'A158fCYNT1A', '부여 성흥산 가림성의 사랑나무 별궤적 야경', '바따구따', TRUE),
    (15, 2, 'r0pS-XgaGKM', '성흥산성 사랑나무 은하수', '희망일출', FALSE),
    (15, 3, 'hJUAkfx5bRo', '부여 성흥산성 - 사랑나무와 별(Love Tree and Star)', 'smartgramer TV', TRUE),
    (18, 0, 'Scv3jKVb2yI', '영양 반딧불이 천문대와 생태공원 사업소', '어디까지가봤니껴', FALSE),
    (18, 1, 'jlnmJTTL7lI', '은하수가 빛나던 영양 밤하늘✨⠀⠀⠀⠀⠀ #영양군 #은하수', '대환장귀촌일기', TRUE),
    (18, 2, '7rlURiMJePw', '하늘에 별이 총총…경북 영양 ''밤하늘보호공원''', '연합뉴스TV', FALSE),
    (18, 3, 'opGmiVAQqhc', '밤하늘의 별처럼 빛나는 반딧불이를 보고싶다면?✨ㅣ이달의 생태관광지- 영양 #밤하늘 #반딧불이 #공원', '기후에너지환경부', FALSE),
    (19, 0, '5HL47ghtf3g', '황매산 은하수 추천 포인트 9곳 [은하수 사진 촬영 장소 / 촬영 방법과 장비] - 1부', '줌살롱', FALSE),
    (19, 1, 'EWW7xhUOuxs', '황매산 은하수 추천 포인트 9곳 [철쭉군락지 일출 / 은하수 사진 추천 장소] - 2부', '줌살롱', FALSE),
    (19, 2, 'MGkENCyUUcA', '#황매산정상주차장#시간별 주차상황#은하수', '걱정인형_Mountain', TRUE),
    (19, 3, 'NHFw1VACGj0', '[여행브이로그]황매산은하수를 만나러 가다! 별들의 성지 합천 황매산! 은하수명소,별명소, 황매산에서 과연 은하수를 만나고 왔을까요?', '안녕,규빛', FALSE),
    (20, 0, 'fBGbPPRDU08', '#R16 북영천IC ~ 보현산천문과학관 (스침) ~ 보현산천문대 가는 길 #영천별빛축제 [안개와 숲으로 길이 안보임]', '휴천기', FALSE),
    (20, 1, 'XugqBO40V14', '[VLOG] | 영천여행 | 보현산 | 천문대 | 별보러가자 | 별똥별 |', '또하루', FALSE),
    (20, 2, 'Zg-ybKix5I4', '보현산 천문대를 간 이유는? 그저 반짝이는 별사진이 찍고 싶어서... (Star Photo TUTORIAL)', '여행미니 - Travel Mini', FALSE),
    (20, 3, '-W1XrbyzzHE', '별사진명소 보현산 천문대 타임랩스 촬영 4K (경상북도 영천시)', '바람 따라 떠나는 여행', FALSE),
    (21, 0, 'fo9TPd2dj_A', '경주풍력발전소에서 무료차박 무료캠핑 | 별빛이 쏟아지는 무료 차박지 에서의 하룻밤 이야기.', '알콜핑', FALSE),
    (21, 1, 'gmQ6KqXBCK4', '지금 경주는 ''우주빛 향연'' 중 ✨🌌그믐밤의 경주, 역대급 은하수 강줄기가 터졌습니다! 📸오늘 문득 마음이 이끌려 달려온 ''경주 풍력발전소(바람의 언덕)'' 현장', '시로코 투어(SiroccoTour)', TRUE),
    (21, 2, 'EZ6vqx25eMU', '경주여행ㅣ당일치기코스ㅣ경주풍력발전소ㅣ별이빛나는밤에', '사랑에빠진하루haru_in_love', FALSE),
    (21, 3, 'OfqAcjTR2zI', '은하수 타입랩스 - 경주 토함산 풍력발전소', '오묘한 별', FALSE),
    (22, 0, 'GQu73BbPeqg', '예약 1분 만에 마감🔥 한우산 별천지, 직접 가보니… #한우산별천지 #의령 #경남여행 #별보기좋은곳', '의령손맛본가TV', TRUE),
    (22, 1, 'MojTw9nRv3E', '올뉴투싼 차박, 경남의 육백마지기! 경남 의령 한우산! 별이 쏟아지는 밤하늘과 멋진 풍경으로 언택트 힐링!', '토닥토닥 떠나요', FALSE),
    (22, 2, '51ZeN_6Jl3U', '의령 한우산 은하수♡별똥별         #한우산은하수 #의령한우산별천지 #여름은하수', '산골새댁애나씨', TRUE),
    (22, 3, 'PZkRxtCs0zg', '경남 의령 한우산 은하수 타임랩스 겨울은하수 4K milkyway night sky Timelapse', 'Night sky films', FALSE),
    (28, 0, 'fngga99pjME', '제주도에서 별보러 1100고지를 간다면 조심하세요..!😂', '개미대장군', TRUE),
    (28, 1, 'RsVOAXZ2HxE', '제주도 별구경 명소 1100고지 | 제주공항&함덕 맛집&카페 | 소노벨 제주 | VOL 4', 'Love Rain', FALSE),
    (28, 2, 'ljHcx8WoQ3Y', '제주 1100고지가 별보기 좋지 않은 이유', '제주부스터', TRUE),
    (28, 3, 'm-SX4rIZEvM', '[제주도] 1100고지 야경 은하수', '조현민', FALSE),
    (30, 0, '4p-8qy-ebO4', '25탄 별보러 간 새별오름', '제주별별TV', TRUE),
    (30, 1, 'ZYNxbBgm0vw', '캠핑카 제주도 여행 노지추천(새별오름)  이국적인 풍경과 쏟아지는 별을 을 느껴보세요', '고디바커피', FALSE),
    (30, 2, 'z7yowha9rvA', '[ENG SUB] 제주도 새별오름의 Day & Night', '재주필름', FALSE),
    (40, 0, 'ebj9n6oLHeE', '[함백산 만항재] 차로 가는 산! 운탄고도의 은하수와 별똥별은 덤!! 차박/노지캠핑/드론/매빅에어2/차박성지', '롯지캠퍼 LodgeCamper', FALSE),
    (40, 1, 'lDcDzAOux-k', '무더위 탈출 / 밤하늘 은하수 / 함백산 일출 / 운해폭포 / 만항재 야생화 범꼬리 / vacance /4k/', '풍경사진연구 박희만 photographer', FALSE),
    (53, 0, '3iLDk75VUfk', '23.04.17 거창 감악산 은하수 타임랩스 촬영(4K)', '택택이 영상 저장공간', FALSE),
    (53, 1, 'swFY2nEUpbA', '[4KUHD] 감악산 은하수', 'ViewT', FALSE),
    (53, 2, 'MJcWYb0bwP4', '감악산은하수', '포토아재', FALSE),
    (53, 3, 'JLFD4pDsbOo', '거창 감악산 무장애 나눔길 하께 걸어요 해발900m 3.5km 1시간 계단없는 사계절 명품 나무테크 하늘길 별바람 언덕 정상 주차장까지 숲길 드라이브#4k #nature', 'Wondering Korea', FALSE),
    (64, 0, 'ZcIdsOPsN0Q', '용눈이오름 근처에서 본 하늘 "일몰", "은하수" 그리고 "별궤적"', '제주도TV·Jeju', FALSE),
    (64, 1, 'qZhrEgsaaAw', '20240414 용눈이오름 은하수관측', '김고래 씨의 돌고래 세상', FALSE),
    (64, 2, 'bvvY-v8AKC0', '용눈이오름에서 본 은하수 4K', 'crom crom', FALSE),
    (65, 0, 'GvMqOrJLtCY', '오도산 전망대 밤하늘 은하수-S25 울트라', '자연멍', TRUE),
    (65, 1, 'cQh_qc-nudQ', '은하수, 운해 맛집 오도산백킹', '간지Tv', FALSE),
    (65, 2, 'tl_o4189vGw', '오도산 에서 본 은하수와 별 궤적 | 타임랩스 | UHD', '인프레임 프로덕션_INFRAME PRODUCTION', FALSE),
    (65, 3, 'r8Vt19cELNc', '우리나라에서도 은하수를 찍을수 있다?  (aka.오도산, 미륵산)', '양타마 타임랩스Timelapse creator', FALSE),
    (67, 0, 'fn_CBEpBHkU', '2024년 5월 20일 제주 한라산 마방목지 밤하늘 별 보러 갔네요 #마방목지 #제주여행 #제주밤하늘 #제주마방목지 #힐링타임 #밤하늘별 #제주도여행가볼만한곳 #야간에갈만한곳', '제주살이 브이로그', TRUE),
    (67, 1, 'RXQJ7FBNuxM', '[생크림TV]캠핑카로 별보러가기ㅣ세모녀의캠핑카여행ㅣ1100고지ㅣ마방목지ㅣ쏠라티캠핑카ㅣ트레블디자인ㅣ트레디ㅣ밴라이프ㅣ', '생크림TV', FALSE),
    (67, 2, '3Qq5Z6_9498', '은하수 마방목지', '두근두근제주', FALSE),
    (68, 0, 'MLmdhqP7fkQ', '지리산 정령치 별빛여행', '구상나무', FALSE),
    (68, 1, 'PDMFWKe4FPw', '[4K Timelapse] 지리산 정령치 은하수', '김현태', FALSE),
    (68, 2, '0IxiJbW2_uE', '정령치 은하수- 디포터 호크마', '호크마종성', FALSE),
    (68, 3, 'hFD_Oz19rG0', '지리산 정령치 은하수 숏', '한국의 은하수 TV Milkyway of Korea TV', FALSE),
    (70, 0, 'rN0kxnpYJg8', '적상산 전망대 : 등산 없이 차로 갈 수 있는 은하수 사진 명소, 운 좋으면 눈으로 보이는 이곳!', '사진담주', FALSE),
    (70, 1, 'R0yN9nqxz7I', '23.09.09 무주 적상산 전망대 은하수 타임랩스', '리아의뷰파인더', FALSE),
    (70, 2, 'IivSPm-7VmQ', '적상산의 별궤적과 체르세우스 유성우', '후니포커스(huny focus)', FALSE),
    (71, 0, '0FfpH4LB4Yw', '거린사슴전망대 별 보러 갈래. 제주 서귀포시 1100로', '꼬꼬두 (GoGoDo) 투어', FALSE),
    (72, 0, 'Vjyt2hI0OUQ', '별이 빛나는 밤에/철원 노동당사', 'banco kim (오마이쥬)', FALSE),
    (72, 1, 'eJ3rgxDTZR4', '전형석의 동영상(노동당사, 백마고지 은하수(2020.05.29))', 'hyungsuk jeon', FALSE),
    (72, 2, 'AMla_ZUHHY0', '철원군 관전리 철원노동당사  2021년07월31일 은하수TV', '은하수TV', FALSE),
    (73, 0, '65fkJzRO-IU', '* 23년12월20일 강화고인돌공원 별보기 *                         (반달~목성~북극성~겨울철 별자리)#강화도#별#고인돌공원', '박주관의 일상풍경', FALSE),
    (73, 1, '7i9Uz88-B7c', '강화 고인돌 별일주', '한동일', FALSE),
    (73, 2, 'CQzRabkRHCI', '강화도 부근리 고인돌  2020년03월07일 은하수TV', '은하수TV', FALSE),
    (73, 3, 'umI2IYpqa-c', '[Timelapse] Star Trails of Dolmen, 강화 고인돌,광성보 북천일주', 'Brian Kim 4K Innocent Gaze', FALSE),
    (74, 0, 'oVgUVSe4Pic', '합천 대암산 활공장 운석 은하수 뷰맛집', '알뜨랑', TRUE),
    (74, 1, '9xZctv6mJZ0', '합천운석공,별이 떨어진 crater 펀치볼, 합천 초계 대암산 은하수 4K milkyway night sky Timelapse', 'Night sky films', FALSE),
    (74, 2, '8-0qETzCkME', '8월 5일 합천 대암산 은하수', '소소티', TRUE),
    (74, 3, 'jJA5oBIRz4Q', '23.08.12 합천 대암산 활공산 은하수 타임랩스', '리아의뷰파인더', FALSE),
    (75, 0, 'UCpd5G303ww', '경북군위 은하수 포인트(화산산성 풍차)', '사진찍는 남자', FALSE),
    (75, 1, '_d3i-cJ-fCE', '군위 화산마을전망대에 별찍으로 갔다가 영상찍고오지요', '낚시한 음악인', FALSE),
    (75, 2, 'kkbHMbf7eYM', '별 🌟일출 ☀️  군위화산산성 전망대 #테슬라모델Y#차박', '시골(countryside)Life', TRUE),
    (75, 3, 'BkbO3gXvmjo', '군위 화산마을 출렁이는 운해와 새벽 여명을 볼 수 있는 숨은 명소 / 차박 / 캠핑장 / 가을여행/Where you can see the Milky Way and cloud sea', '폴라리스 Polaris', FALSE),
    (76, 0, 'wxck5yENzoE', '의성 달빛공원 (ㄹㅇ별 쏟아짐)', '의성TV', FALSE),
    (76, 1, 'Dgqcbf3sElo', '의성달빛공원, 밤하늘 명소로 정비 / 안동MBC', '안동MBC NEWS', FALSE),
    (76, 2, 'JHBLuiMD5tc', '의성 달빛공원 은하수 타임랩스', '은하수방랑가의 탐구생활', FALSE),
    (77, 0, 'orxo5VVywfo', '2019년 8월 3일 맹동산 은하수 타임랩스(영양풍력발전단지)', '김석현', FALSE),
    (77, 1, 'J4cx7sMPWxI', '경북 영양군 맹동산 은하수와 페르세우스 유성우 Milkyway and Persaid Meteor Shower in Maengdong Mountain Yeongyang Korea', '한국의 은하수 TV Milkyway of Korea TV', FALSE),
    (77, 2, 'NE_djdAf9TY', '갤럭시 폰으로 촬영한 영양 맹동산 은하수 타임랩스 쇼츠 영상 Milkyway in Maengdong Mountain Yeongyang with Galaxy phone shorts', '한국의 은하수 TV Milkyway of Korea TV', TRUE),
    (78, 0, 'LG3UkncdcMA', '춘천 건봉령 승호대 달과 은하수', '청암 tv', FALSE),
    (78, 1, 'vnN5qexEXt0', '춘천 건봉령 승호대 은하수', '희망일출', FALSE),
    (78, 2, 'aLBwOe8RwH0', '강원도 건봉령 승호대에서 담은 은하수 타임랩스 | 2025.03.20-21', '라크네 사진관', FALSE),
    (78, 3, 'EZmnMoa9PoA', '건봉령 승호대 (춘천 소양호) 은하수 타임랩스', '별보러 갑니다', FALSE),
    (79, 0, '58Mc7gCMmtA', '부안 솔섬 은하수 타임랩스', '리얼베어스냅', FALSE),
    (79, 1, 'jELk8meSpBs', '부안 솔섬의 밤하늘 별 빛의 흔적 들', '신재용', FALSE),
    (79, 2, '9H4jaYAryi4', 'Milky way time lapse in South Korea  한국의 은하수 / 부안 / 변산반도 / 솔섬 / 타임랩스', '안형수 | 교정전문의 Orthodontist', FALSE),
    (80, 0, 'b1WBB-rkdOY', '철원 백마고지 전적비 별과 은하수 Time Lapse', '청암 tv', FALSE),
    (80, 1, 'DvaCkbBnkw4', '백마고지 별 촬영⭐️', '현수쨩TV', TRUE),
    (80, 2, 'wbA7srjB5SQ', '4K 백마고지의 잠든 밤을 깨우는 별들의 댄스 l Star Trails: A Vigil of Light #철원 #백마고지 #별궤적 #북천일주 #타임랩스 #StarTrails', 'Brian Kim 4K Innocent Gaze', TRUE),
    (80, 3, 'HLsMtCz388Q', '철원 백마고지 은하수 타임랩스', 'banco kim (오마이쥬)', FALSE),
    (82, 0, 'fbstLl8PYV0', '밀양 얼음골 별 구름 은하수 타임랩스 영상 Stars and Cloud timelapse in Uleomgol Milyang Korea', '한국의 은하수 TV Milkyway of Korea TV', FALSE),
    (82, 1, 'F-KKshl3pxc', '밀양 얼음골에서 별 구름 은하수 타임랩스 쇼츠 Milkyway and Cloud timelapse in Uleomgol Milyang shorts', '한국의 은하수 TV Milkyway of Korea TV', TRUE),
    (86, 0, 'InSAyqx1X5o', '풍점고개 & 원주 거돈사지 은하수 출사지 촬영 포인트 파헤치기', '청암 tv', FALSE),
    (86, 1, 'oipwB-7Lz7E', '3월~6월 강원도 원주 은하수 포인트(거돈사지)', '사진찍는 남자', FALSE),
    (86, 2, '74VXIyF1QZw', '은하수 타임랩스 사진 촬영 (국내여행 강원도 원주 거돈사지)', '상훈파크', FALSE),
    (86, 3, 'c3HnQNLvQHI', '거돈사지 별궤적 타임랩스 4K, Star trail Time lapse', '여행 사진 작가 유승훈', FALSE),
    (87, 0, 'SeP0Hhto-QA', '전형석의 동영상(고성, 상족암의 은하수(2020.04.24))', 'hyungsuk jeon', FALSE),
    (87, 1, 'dcVvgryP2rU', '고성상족암오토캠핑장에서 별 + 일출', 'Yosiddang', TRUE),
    (88, 0, 'C6m2eHj1UiE', '남해 다랭이마을 은하수 + 일출', '뚱구리', FALSE),
    (88, 1, 'ML2geE8wafc', '남해군 남면 홍현리 다랭이마을  2025년07월06일 은하수TV', '은하수TV', FALSE),
    (89, 0, 'OtNx8DpsBtg', '시네마틱 영상/함양 오도재 지안재 백패킹/별타임랩스', '사진가정민호', FALSE),
    (89, 1, 'o5pZxDOKCOc', '함양 오도재에서 지리산을 횡단하는 은하수 타임랩스 보정본 쇼츠 영상 Milkyway on the Jiri Mountain Hamyang Korea', '한국의 은하수 TV Milkyway of Korea TV', TRUE);
