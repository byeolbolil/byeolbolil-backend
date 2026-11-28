package xyz.ncookie.stargazer.domain.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import xyz.ncookie.stargazer.domain.member.entity.Member;
import xyz.ncookie.stargazer.domain.member.entity.AuthProvider;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByEmailAndAuthProvider(String email, AuthProvider authProvider);

	Optional<Member> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

	boolean existsByEmailAndAuthProvider(String email, AuthProvider authProvider);

	List<Member> findAllByEmail(String email);

	// 비교는 컬럼의 콜레이션(운영과 테스트 모두 utf8mb4_unicode_ci)을 따라 대소문자를 가리지 않는다
	boolean existsByNickname(String nickname);

	boolean existsByNicknameAndIdNot(String nickname, Long id);
}
