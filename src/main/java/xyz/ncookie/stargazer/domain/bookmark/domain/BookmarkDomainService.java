package xyz.ncookie.stargazer.domain.bookmark.domain;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.ncookie.stargazer.domain.bookmark.entity.Bookmark;
import xyz.ncookie.stargazer.domain.bookmark.exception.BookmarkErrorCode;
import xyz.ncookie.stargazer.domain.bookmark.exception.BookmarkException;
import xyz.ncookie.stargazer.domain.bookmark.repository.BookmarkRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookmarkDomainService {

	private final BookmarkRepository bookmarkRepository;

	public Bookmark findById(Long bookmarkId) {

		return bookmarkRepository.findById(bookmarkId)
			.orElseThrow(() -> new BookmarkException(BookmarkErrorCode.BOOKMARK_NOT_FOUND, bookmarkId.toString()));
	}

	public List<Bookmark> findAllByMemberId(Long memberId) {
		return bookmarkRepository.findAllByMember_Id(memberId);
	}

	public void validateOwner(Long memberId, Bookmark bookmark) {

		if (!bookmark.isOwner(memberId)) {
			throw new BookmarkException(BookmarkErrorCode.BOOKMARK_FORBIDDEN);
		}
	}

	public void updateBookmark(Bookmark bookmark, String name, String memo) {
		bookmark.updateBookmark(name, memo);
	}

	public void deleteBookmark(Bookmark bookmark) {
		bookmarkRepository.delete(bookmark);
	}

	/** 탈퇴하는 회원의 북마크. 북마크가 회원을 가리키므로 회원보다 먼저 지운다 */
	public void deleteAllByMemberId(Long memberId) {
		bookmarkRepository.deleteAllByMember_Id(memberId);
	}
}
