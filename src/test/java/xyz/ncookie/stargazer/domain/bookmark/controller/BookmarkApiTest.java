package xyz.ncookie.stargazer.domain.bookmark.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;
import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.repository.MemberRepository;
import xyz.ncookie.stargazer.global.security.jwt.JwtTokenProvider;
import xyz.ncookie.stargazer.support.IntegrationTestSupport;

/**
 * 북마크(내 장소) API의 응답을 고정한다.
 *
 * SPOT은 data.sql로 들어가는 명소를 가리킨다. 1번은 강릉 안반데기다.
 * 응답의 좌표와 주소는 SPOT이면 명소의 값, CUSTOM이면 저장할 때 보낸 값이다.
 */
class BookmarkApiTest extends IntegrationTestSupport {

	private static final String CUSTOM = """
		{
		  "type": "CUSTOM",
		  "name": "우리집 옥상",
		  "latitude": 37.55,
		  "longitude": 126.91,
		  "address": "서울 마포구",
		  "memo": "남쪽이 트여 있다"
		}
		""";

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private BookmarkRepository bookmarkRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	private Member member;
	private Member stranger;

	@BeforeEach
	void setUp() {
		bookmarkRepository.deleteAllInBatch();
		memberRepository.deleteAllInBatch();
		member = memberRepository.save(Member.local("stargazer@example.com", "password", "별지기"));
		stranger = memberRepository.save(Member.local("stranger@example.com", "password", "지나가는 사람"));
	}

	@Test
	@DisplayName("로그인하지 않으면 401이다")
	void requiresLogin() {
		assertThat(mvc.get().uri("/api/v1/bookmarks").exchange()).hasStatus(401);
	}

	@Nested
	@DisplayName("POST /api/v1/bookmarks")
	class Add {

		@Test
		@DisplayName("CUSTOM은 보낸 좌표와 주소를 그대로 저장한다. spotId는 null로 나간다")
		void custom() {
			MvcTestResult result = add(member, CUSTOM);

			assertThat(result).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다.",
				  "data": {
				    "bookmarkId": %d,
				    "type": "CUSTOM",
				    "spotId": null,
				    "name": "우리집 옥상",
				    "latitude": 37.55,
				    "longitude": 126.91,
				    "address": "서울 마포구",
				    "memo": "남쪽이 트여 있다"
				  }
				}
				""".formatted(idOf(result)));
		}

		@Test
		@DisplayName("SPOT은 좌표와 주소를 명소에서 가져온다. 요청에 넣은 좌표와 주소는 쓰지 않는다")
		void spot() {
			MvcTestResult result = add(member, """
				{ "type": "SPOT", "spotId": 1, "name": "안반데기", "latitude": 1.0, "longitude": 2.0, "address": "무시된다" }
				""");

			assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{
				  "data": {
				    "type": "SPOT",
				    "spotId": 1,
				    "name": "안반데기",
				    "latitude": 37.6254,
				    "longitude": 128.9392,
				    "address": "강원도 강릉시 왕산면 대기리 2214-187",
				    "memo": null
				  }
				}
				""");
		}

		@Test
		@DisplayName("같은 명소를 두 번 저장하면 409다")
		void duplicateSpot() {
			add(member, """
				{ "type": "SPOT", "spotId": 1, "name": "안반데기" }
				""");

			MvcTestResult result = add(member, """
				{ "type": "SPOT", "spotId": 1, "name": "안반데기 한 번 더" }
				""");

			assertThat(result).hasStatus(409).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 409,
				  "message": "이미 해당 명소는 북마크 되어있습니다."
				}
				""");
		}

		@Test
		@DisplayName("없는 명소는 404다")
		void unknownSpot() {
			MvcTestResult result = add(member, """
				{ "type": "SPOT", "spotId": 999999, "name": "없는 명소" }
				""");

			assertThat(result).hasStatus(404).bodyJson()
				.extractingPath("$.message").isEqualTo("해당 ID의 명소 데이터를 찾을 수 없습니다.");
		}

		@Test
		@DisplayName("type과 name이 빠지면 400이고 data에 필드별 사유가 담긴다")
		void missingFields() {
			assertThat(add(member, "{}")).hasStatus(400).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 400,
				  "message": "유효성 검사에 실패하였습니다.",
				  "data": {
				    "type": "북마크 타입은 필수입니다.",
				    "name": "북마크 이름은 필수입니다."
				  }
				}
				""");
		}

		@Test
		@DisplayName("타입에 맞는 값이 빠지면 400이고 사유의 키는 필드 이름이 아니라 spotValid, customValid다")
		void typeSpecificFields() {
			assertThat(add(member, """
				{ "type": "SPOT", "name": "명소 ID 없음" }
				""")).hasStatus(400).bodyJson()
				.extractingPath("$.data.spotValid").isEqualTo("SPOT 타입은 spotId가 필수입니다.");

			assertThat(add(member, """
				{ "type": "CUSTOM", "name": "좌표 없음" }
				""")).hasStatus(400).bodyJson()
				.extractingPath("$.data.customValid").isEqualTo("CUSTOM 타입은 좌표(latitude, longitude)가 필수입니다.");
		}

		@Test
		@DisplayName("CUSTOM은 주소도 있어야 한다. 빠지면 400인데 data 없이 메시지만 온다")
		void customWithoutAddress() {
			MvcTestResult result = add(member, """
				{ "type": "CUSTOM", "name": "주소 없음", "latitude": 37.5, "longitude": 127.0 }
				""");

			assertThat(result).hasStatus(400).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 400,
				  "message": "북마크 타입과 데이터가 일치하지 않습니다."
				}
				""");
		}

		@Test
		@DisplayName("type에 없는 값을 보내면 400이다")
		void unknownType() {
			MvcTestResult result = add(member, """
				{ "type": "OTHER", "name": "모르는 타입" }
				""");

			assertThat(result).hasStatus(400).bodyJson()
				.extractingPath("$.message").isEqualTo("요청 본문을 읽을 수 없습니다.");
		}
	}

	@Nested
	@DisplayName("GET /api/v1/bookmarks")
	class ListMine {

		@Test
		@DisplayName("저장한 것이 없으면 빈 배열이다")
		void empty() {
			assertThat(list(member)).hasStatusOk().bodyJson()
				.extractingPath("$.data").asArray().isEmpty();
		}

		@Test
		@DisplayName("내 것만 나온다")
		void onlyMine() {
			add(member, CUSTOM);
			add(stranger, CUSTOM);

			assertThat(list(member)).hasStatusOk().bodyJson()
				.extractingPath("$.data").asArray().hasSize(1);
		}
	}

	@Nested
	@DisplayName("PATCH /api/v1/bookmarks/{bookmarkId}")
	class Modify {

		private long bookmarkId;

		@BeforeEach
		void saved() {
			bookmarkId = idOf(add(member, CUSTOM));
		}

		@Test
		@DisplayName("이름과 메모를 바꾼다")
		void nameAndMemo() {
			MvcTestResult result = modify(member, bookmarkId, """
				{ "name": "옥상", "memo": "난간 조심" }
				""");

			assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{ "data": { "name": "옥상", "memo": "난간 조심", "latitude": 37.55, "address": "서울 마포구" } }
				""");
		}

		@Test
		@DisplayName("memo를 빼고 보내면 메모가 지워진다")
		void omittedMemoIsCleared() {
			MvcTestResult result = modify(member, bookmarkId, """
				{ "name": "옥상" }
				""");

			assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{ "data": { "name": "옥상", "memo": null } }
				""");
		}

		@Test
		@DisplayName("name이 빠지면 400이다")
		void nameIsRequired() {
			MvcTestResult result = modify(member, bookmarkId, """
				{ "memo": "이름 없이" }
				""");

			assertThat(result).hasStatus(400).bodyJson()
				.extractingPath("$.data.name").isEqualTo("수정할 북마크 이름은 필수입니다.");
		}

		@Test
		@DisplayName("남의 북마크는 403이다")
		void notOwner() {
			MvcTestResult result = modify(stranger, bookmarkId, """
				{ "name": "남의 것" }
				""");

			assertThat(result).hasStatus(403).bodyJson().isStrictlyEqualTo("""
				{
				  "success": false,
				  "status": 403,
				  "message": "해당 북마크를 수정 또는 삭제할 권한이 없습니다."
				}
				""");
		}

		@Test
		@DisplayName("없는 북마크는 404다")
		void unknown() {
			MvcTestResult result = modify(member, 999999, """
				{ "name": "없는 것" }
				""");

			assertThat(result).hasStatus(404).bodyJson()
				.extractingPath("$.message").isEqualTo("해당 ID의 북마크 데이터를 찾을 수 없습니다.");
		}
	}

	@Nested
	@DisplayName("DELETE /api/v1/bookmarks/{bookmarkId}")
	class Remove {

		private long bookmarkId;

		@BeforeEach
		void saved() {
			bookmarkId = idOf(add(member, CUSTOM));
		}

		@Test
		@DisplayName("지우면 data 없이 200이고, 다시 지우면 404다")
		void removes() {
			assertThat(remove(member, bookmarkId)).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "success": true,
				  "status": 200,
				  "message": "요청이 성공적으로 처리되었습니다."
				}
				""");
			assertThat(remove(member, bookmarkId)).hasStatus(404);
		}

		@Test
		@DisplayName("남의 북마크는 403이고 지워지지 않는다")
		void notOwner() {
			assertThat(remove(stranger, bookmarkId)).hasStatus(403);
			assertThat(list(member)).bodyJson().extractingPath("$.data").asArray().hasSize(1);
		}
	}

	// ---- 준비 ----

	private MvcTestResult add(Member who, String body) {
		return mvc.post().uri("/api/v1/bookmarks")
			.header("Authorization", bearer(who))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	private MvcTestResult list(Member who) {
		return mvc.get().uri("/api/v1/bookmarks").header("Authorization", bearer(who)).exchange();
	}

	private MvcTestResult modify(Member who, long bookmarkId, String body) {
		return mvc.patch().uri("/api/v1/bookmarks/" + bookmarkId)
			.header("Authorization", bearer(who))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	private MvcTestResult remove(Member who, long bookmarkId) {
		return mvc.delete().uri("/api/v1/bookmarks/" + bookmarkId).header("Authorization", bearer(who)).exchange();
	}

	private String bearer(Member who) {
		return "Bearer " + jwtTokenProvider.createAccessToken(who.getId());
	}

	private static long idOf(MvcTestResult result) {
		try {
			Number id = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.bookmarkId");
			return id.longValue();
		} catch (java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}
