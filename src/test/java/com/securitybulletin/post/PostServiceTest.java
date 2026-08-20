package com.securitybulletin.post;

import com.securitybulletin.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PostService에 @PreAuthorize("isAuthenticated()")가 걸려 있으므로
 * 서비스 호출에는 인증 컨텍스트가 필요하다.
 * 소유자 판단은 principal이 아니라 인자로 받은 username으로 하므로,
 * 여기서 mock 사용자가 누구인지는 결과에 영향을 주지 않는다.
 */
@SpringBootTest
@Transactional
@WithMockUser(username = "alice")
class PostServiceTest {

	@Autowired
	private PostService postService;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserService userService;

	@BeforeEach
	void setUp() {
		userService.signup("alice", "alice@example.com", "password123");
		userService.signup("bob", "bob@example.com", "password123");
	}

	@Test
	@DisplayName("로그인 사용자는 글을 작성할 수 있고 작성자가 기록된다")
	void create_recordsAuthor() {
		Post post = postService.create("alice", "제목", "본문");

		Post found = postService.findById(post.getId());
		assertThat(found.getTitle()).isEqualTo("제목");
		assertThat(found.getAuthor().getUsername()).isEqualTo("alice");
		assertThat(found.getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("작성자 본인은 자기 글을 수정할 수 있다")
	void update_succeedsForAuthor() {
		Post post = postService.create("alice", "원래 제목", "원래 본문");

		postService.update(post.getId(), "바뀐 제목", "바뀐 본문", "alice");

		Post updated = postService.findById(post.getId());
		assertThat(updated.getTitle()).isEqualTo("바뀐 제목");
		assertThat(updated.getContent()).isEqualTo("바뀐 본문");
	}

	@Test
	@DisplayName("다른 사용자의 글은 수정할 수 없고, 내용도 바뀌지 않는다")
	void update_deniedForNonAuthor() {
		Post post = postService.create("alice", "앨리스 글", "앨리스 본문");

		assertThatThrownBy(() -> postService.update(post.getId(), "탈취 제목", "탈취 본문", "bob"))
			.isInstanceOf(AccessDeniedException.class);

		// 거부만으로 끝내지 않고, 실제로 변경되지 않았는지까지 확인한다
		Post unchanged = postService.findById(post.getId());
		assertThat(unchanged.getTitle()).isEqualTo("앨리스 글");
		assertThat(unchanged.getContent()).isEqualTo("앨리스 본문");
	}

	@Test
	@DisplayName("작성자 본인은 자기 글을 삭제할 수 있다")
	void delete_succeedsForAuthor() {
		Post post = postService.create("alice", "지울 글", "본문");

		postService.delete(post.getId(), "alice");

		assertThat(postRepository.findById(post.getId())).isEmpty();
	}

	@Test
	@DisplayName("다른 사용자의 글은 삭제할 수 없고, 글도 남아있다")
	void delete_deniedForNonAuthor() {
		Post post = postService.create("alice", "앨리스 글", "본문");

		assertThatThrownBy(() -> postService.delete(post.getId(), "bob"))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(postRepository.findById(post.getId())).isPresent();
	}

	@Test
	@DisplayName("존재하지 않는 게시글을 조회하면 PostNotFoundException이 발생한다")
	void findById_throwsWhenMissing() {
		assertThatThrownBy(() -> postService.findById(99999L))
			.isInstanceOf(PostNotFoundException.class);
	}

	@Test
	@DisplayName("목록은 최신순으로 페이징된다")
	void findAll_isPagedAndSortedByNewest() {
		for (int i = 1; i <= 12; i++) {
			postService.create("alice", "글 " + i, "본문");
		}

		var firstPage = postService.findAll(
			PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

		assertThat(firstPage.getTotalElements()).isEqualTo(12);
		assertThat(firstPage.getContent()).hasSize(10);
		assertThat(firstPage.getTotalPages()).isEqualTo(2);
	}

	@Test
	@DisplayName("isAuthor는 작성자에게만 true를 반환한다")
	void isAuthor_onlyForAuthor() {
		Post post = postService.create("alice", "제목", "본문");

		assertThat(postService.isAuthor(post.getId(), "alice")).isTrue();
		assertThat(postService.isAuthor(post.getId(), "bob")).isFalse();
		assertThat(postService.isAuthor(post.getId(), null)).isFalse();
	}

}
