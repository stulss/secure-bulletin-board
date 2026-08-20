package com.securitybulletin;

import com.securitybulletin.post.Post;
import com.securitybulletin.post.PostService;
import com.securitybulletin.user.UserRepository;
import com.securitybulletin.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;

/**
 * 이 프로젝트의 존재 이유인 방어 장치들을 한곳에서 확인한다.
 * 개별 기능 테스트(UserServiceTest, PostControllerTest 등)와 중복되더라도,
 * "보안 요구사항"이라는 관점에서 한 파일로 모아 읽을 수 있게 둔다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityDefenseTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PostService postService;

	@BeforeEach
	void setUp() {
		userService.signup("victim", "victim@example.com", "password123");
	}

	@Test
	@DisplayName("SQL Injection: 로그인 아이디에 ' OR '1'='1 을 넣어도 인증되지 않는다")
	void sqlInjection_inLoginForm_doesNotAuthenticate() throws Exception {
		mockMvc.perform(formLogin("/auth/login")
				.user("' OR '1'='1")
				.password("' OR '1'='1"))
			.andExpect(unauthenticated());
	}

	@Test
	@DisplayName("SQL Injection: 조회 조건에 구문을 넣어도 테이블이 삭제되거나 다른 행이 새지 않는다")
	void sqlInjection_inRepositoryQuery_isTreatedAsLiteral() {
		// JPA 파라미터 바인딩이므로 아래 문자열은 '값'으로만 취급된다
		String payload = "victim'; DROP TABLE users; --";

		assertThat(userRepository.findByUsername(payload)).isEmpty();
		assertThat(userRepository.existsByUsername(payload)).isFalse();

		// 테이블이 살아있고 기존 사용자도 그대로인지 확인
		assertThat(userRepository.findByUsername("victim")).isPresent();
		assertThat(userRepository.count()).isEqualTo(1);
	}

	@Test
	@WithMockUser(username = "victim")
	@DisplayName("XSS: 제목과 본문 모두 출력 시 이스케이프된다")
	void xss_inTitleAndContent_isEscaped() throws Exception {
		Post post = postService.create("victim",
			"<img src=x onerror=alert(1)>",
			"<script>alert('xss')</script>");

		String html = mockMvc.perform(
				org.springframework.test.web.servlet.request.MockMvcRequestBuilders
					.get("/posts/" + post.getId()))
			.andReturn().getResponse().getContentAsString();

		assertThat(html).doesNotContain("<img src=x onerror=alert(1)>");
		assertThat(html).doesNotContain("<script>alert('xss')</script>");
		assertThat(html).contains("&lt;img src=x onerror=alert(1)&gt;");
	}

	@Test
	@DisplayName("메서드 보안: 인증 컨텍스트 없이 Service를 직접 호출하면 거부된다")
	void methodSecurity_blocksUnauthenticatedServiceCall() {
		// Controller를 거치지 않고 Service를 직접 호출하는 경로에서도 막히는지 확인.
		// (@PreAuthorize("isAuthenticated()") 가 걸려 있다)
		assertThatThrownBy(() -> postService.create("victim", "제목", "본문"))
			.isInstanceOfAny(AccessDeniedException.class,
				AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	@DisplayName("BCrypt: 저장된 비밀번호로는 원래 값을 알 수 없다")
	void bcrypt_storedHashIsNotReversible() {
		String stored = userRepository.findByUsername("victim").orElseThrow().getPassword();

		assertThat(stored).isNotEqualTo("password123");
		assertThat(stored).doesNotContain("password123");
		assertThat(stored).startsWith("$2");
		assertThat(stored).hasSize(60);
	}

}
