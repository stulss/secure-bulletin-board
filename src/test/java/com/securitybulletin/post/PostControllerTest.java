package com.securitybulletin.post;

import com.securitybulletin.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostService postService;

	@Autowired
	private UserService userService;

	@BeforeEach
	void setUp() {
		userService.signup("alice", "alice@example.com", "password123");
		userService.signup("bob", "bob@example.com", "password123");
	}

	@Test
	@DisplayName("비로그인 상태에서도 글 목록을 열람할 수 있다")
	void list_isPublic() throws Exception {
		mockMvc.perform(get("/posts"))
			.andExpect(status().isOk())
			.andExpect(view().name("post/list"));
	}

	@Test
	@WithMockUser(username = "alice")   // 글을 만들려면 인증이 필요하다
	@DisplayName("비로그인 상태에서도 글 상세를 열람할 수 있다")
	void detail_isPublic() throws Exception {
		Post post = postService.create("alice", "공개 글", "본문");

		// 조회 요청만 익명으로 보낸다
		mockMvc.perform(get("/posts/" + post.getId()).with(anonymous()))
			.andExpect(status().isOk())
			.andExpect(view().name("post/detail"));
	}

	@Test
	@DisplayName("비로그인 상태로 글쓰기 페이지에 접근하면 로그인 화면으로 리다이렉트된다")
	void createForm_requiresLogin() throws Exception {
		mockMvc.perform(get("/posts/new"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("**/auth/login"));
	}

	@Test
	@DisplayName("비로그인 상태로 글을 작성하면 거부된다")
	void create_requiresLogin() throws Exception {
		mockMvc.perform(post("/posts")
				.with(csrf())
				.param("title", "무단 작성")
				.param("content", "본문"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("**/auth/login"));
	}

	@Test
	@WithMockUser(username = "alice")
	@DisplayName("CSRF 토큰 없이 글을 작성하면 403으로 거부된다")
	void create_rejectedWithoutCsrf() throws Exception {
		mockMvc.perform(post("/posts")
				.param("title", "토큰 없음")
				.param("content", "본문"))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "alice")
	@DisplayName("로그인 상태에서 글을 작성하면 상세 페이지로 이동한다")
	void create_succeedsWhenAuthenticated() throws Exception {
		mockMvc.perform(post("/posts")
				.with(csrf())
				.param("title", "새 글")
				.param("content", "본문"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("/posts/*"));
	}

	@Test
	@WithMockUser(username = "alice")
	@DisplayName("제목이 비어 있으면 저장되지 않고 폼으로 되돌아온다")
	void create_failsValidationWhenTitleBlank() throws Exception {
		mockMvc.perform(post("/posts")
				.with(csrf())
				.param("title", "")
				.param("content", "본문"))
			.andExpect(status().isOk())
			.andExpect(view().name("post/form"))
			.andExpect(model().attributeHasFieldErrors("postForm", "title"));
	}

	@Test
	@WithMockUser(username = "bob")
	@DisplayName("다른 사용자의 글 ID로 수정 URL을 직접 호출하면 403이고, 내용도 바뀌지 않는다")
	void edit_deniedForNonAuthor() throws Exception {
		Post post = postService.create("alice", "앨리스 글", "앨리스 본문");

		mockMvc.perform(post("/posts/" + post.getId() + "/edit")
				.with(csrf())
				.param("title", "탈취 제목")
				.param("content", "탈취 본문"))
			.andExpect(status().isForbidden());

		Post unchanged = postService.findById(post.getId());
		assertThat(unchanged.getTitle()).isEqualTo("앨리스 글");
	}

	@Test
	@WithMockUser(username = "bob")
	@DisplayName("다른 사용자의 글 수정 폼에 접근하면 403이다")
	void editForm_deniedForNonAuthor() throws Exception {
		Post post = postService.create("alice", "앨리스 글", "본문");

		mockMvc.perform(get("/posts/" + post.getId() + "/edit"))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "bob")
	@DisplayName("다른 사용자의 글을 삭제 URL로 직접 호출하면 403이고, 글도 남아있다")
	void delete_deniedForNonAuthor() throws Exception {
		Post post = postService.create("alice", "앨리스 글", "본문");

		mockMvc.perform(post("/posts/" + post.getId() + "/delete").with(csrf()))
			.andExpect(status().isForbidden());

		assertThat(postService.findById(post.getId())).isNotNull();
	}

	@Test
	@WithMockUser(username = "alice")
	@DisplayName("본인 글은 수정·삭제할 수 있다")
	void editAndDelete_succeedForAuthor() throws Exception {
		Post post = postService.create("alice", "원래 제목", "본문");

		mockMvc.perform(post("/posts/" + post.getId() + "/edit")
				.with(csrf())
				.param("title", "바뀐 제목")
				.param("content", "바뀐 본문"))
			.andExpect(redirectedUrl("/posts/" + post.getId()));

		assertThat(postService.findById(post.getId()).getTitle()).isEqualTo("바뀐 제목");

		mockMvc.perform(post("/posts/" + post.getId() + "/delete").with(csrf()))
			.andExpect(redirectedUrl("/posts"));
	}

	@Test
	@DisplayName("존재하지 않는 글을 조회하면 404가 반환된다")
	void detail_returns404WhenMissing() throws Exception {
		mockMvc.perform(get("/posts/99999"))
			.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "alice")
	@DisplayName("본문에 script 태그를 넣어 저장해도 출력 시 이스케이프되어 실행되지 않는다")
	void xssPayload_isEscapedInOutput() throws Exception {
		Post post = postService.create("alice", "XSS 시도", "<script>alert(1)</script>");

		String html = mockMvc.perform(get("/posts/" + post.getId()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		// 원본 태그가 그대로 나가면 브라우저가 실행한다 — 그렇지 않아야 한다
		assertThat(html).doesNotContain("<script>alert(1)</script>");
		// 이스케이프된 형태로 화면에 보인다
		assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
	}

}
