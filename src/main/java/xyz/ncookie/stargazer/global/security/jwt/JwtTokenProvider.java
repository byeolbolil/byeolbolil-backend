package xyz.ncookie.stargazer.global.security.jwt;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import xyz.ncookie.stargazer.domain.member.entity.Role;

@Component
public class JwtTokenProvider {

	private final Key key;

	public static final String BEARER_PREFIX = "Bearer ";
	public static final long ACCESS_EXPIRE_MS = 1000L * 60 * 15;     // 15분
	public static final long REFRESH_EXPIRE_MS = 1000L * 60 * 60 * 24 * 14; // 14일

	private static final String TYPE_CLAIM = "type";
	private static final String TYPE_ACCESS = "access";
	private static final String TYPE_REFRESH = "refresh";

	public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {

		byte[] keyBytes = Base64.getDecoder().decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyBytes);
	}

	public String createAccessToken(Long memberId) {

		Date now = new Date();

		return Jwts.builder()
			.setSubject(memberId.toString())
			.claim("role", Role.USER)
			.claim(TYPE_CLAIM, TYPE_ACCESS)
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + ACCESS_EXPIRE_MS))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	public String createRefreshToken(Long memberId) {

		Date now = new Date();

		// 발급 시각이 초 단위라 같은 초에 만든 토큰은 문자열이 같아진다. 그러면 재발급 때 지운 토큰과 새 토큰이
		// 구분되지 않으므로 토큰마다 고유한 ID를 넣는다.
		return Jwts.builder()
			.setId(UUID.randomUUID().toString())
			.setSubject(memberId.toString())
			.claim("role", Role.USER)
			.claim(TYPE_CLAIM, TYPE_REFRESH)
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + REFRESH_EXPIRE_MS))
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	/**
	 * 두 토큰은 같은 키로 서명하므로 종류를 확인하지 않으면 리프레시 토큰이 액세스 토큰 자리에서도 통한다.
	 * 그러면 로그아웃한 뒤에도 리프레시 토큰의 남은 수명만큼 API를 쓸 수 있다.
	 */
	public boolean isAccessToken(Claims claims) {
		return TYPE_ACCESS.equals(claims.get(TYPE_CLAIM, String.class));
	}

	public Claims extractClaims(String token) {

		return Jwts.parserBuilder().setSigningKey(key).build()
			.parseClaimsJws(token)
			.getBody();
	}

	public boolean validate(String token) {

		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (io.jsonwebtoken.JwtException e) {
			return false;
		}
	}

	public String substringToken(String token) {

		if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
			return token.substring(BEARER_PREFIX.length());
		}

		return null;
	}
}
