package com.securitybulletin.post;

import com.securitybulletin.user.User;
import com.securitybulletin.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public Page<Post> findAll(Pageable pageable) {
		return postRepository.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public Post findById(Long postId) {
		return postRepository.findById(postId)
			.orElseThrow(() -> new PostNotFoundException(postId));
	}

	/**
	 * @PreAuthorize 는 "로그인했는가"까지만 본다.
	 * "본인 글인가"는 아래 update/delete 안의 requireAuthor()가 판단한다 — 두 질문을 섞지 않는다.
	 */
	@PreAuthorize("isAuthenticated()")
	@Transactional
	public Post create(String username, String title, String content) {
		User author = requireUser(username);

		Post post = Post.builder()
			.author(author)
			.title(title)
			.content(content)
			.build();

		return postRepository.save(post);
	}

	/**
	 * 게시글 수정. 작성자 본인만 가능하다.
	 *
	 * 이 검증을 Controller나 화면(버튼 숨김)에만 두면 URL로 직접 호출해 우회할 수 있으므로,
	 * 어떤 경로로 들어오든 반드시 통과하는 Service 계층에 둔다.
	 */
	@PreAuthorize("isAuthenticated()")
	@Transactional
	public Post update(Long postId, String title, String content, String username) {
		Post post = findById(postId);
		requireAuthor(post, username);

		post.setTitle(title);
		post.setContent(content);
		return post;   // 영속 상태이므로 트랜잭션 종료 시 변경 감지로 반영된다
	}

	/** 게시글 삭제. 작성자 본인만 가능하다. */
	@PreAuthorize("isAuthenticated()")
	@Transactional
	public void delete(Long postId, String username) {
		Post post = findById(postId);
		requireAuthor(post, username);

		postRepository.delete(post);
	}

	/** 주어진 사용자가 이 글의 작성자인지 (화면에서 수정/삭제 버튼 노출 여부 판단용) */
	@Transactional(readOnly = true)
	public boolean isAuthor(Long postId, String username) {
		if (username == null) {
			return false;
		}
		return userRepository.findByUsername(username)
			.map(user -> findById(postId).isAuthoredBy(user.getId()))
			.orElse(false);
	}

	/**
	 * 작성자가 아니면 AccessDeniedException을 던진다.
	 * 수정 폼을 열기 전처럼, 아직 변경은 없지만 접근 자체를 막아야 할 때 쓴다.
	 */
	@Transactional(readOnly = true)
	public void requireAuthorOrThrow(Long postId, String username) {
		requireAuthor(findById(postId), username);
	}

	private User requireUser(String username) {
		return userRepository.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
	}

	private void requireAuthor(Post post, String username) {
		User user = requireUser(username);
		if (!post.isAuthoredBy(user.getId())) {
			throw new AccessDeniedException("본인 게시글만 수정·삭제할 수 있습니다.");
		}
	}

}
