package com.securitybulletin.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserService userService;

	@Test
	@DisplayName("인증 없이 보호된 경로에 접근하면 로그인 페이지로 리다이렉트된다")
	void protectedEndpoint_redirectsToLogin_whenAnonymous() throws Exception {
		mockMvc.perform(get("/auth/me"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("**/auth/login"));
	}

	@Test
	@DisplayName("CSRF 토큰 없이 회원가입을 POST하면 403으로 거부된다")
	void signup_rejectedWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/auth/signup")
				.param("username", "csrfuser")
				.param("email", "csrf@example.com")
				.param("password", "password123")
				.param("passwordConfirm", "password123"))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("CSRF 토큰이 있으면 회원가입에 성공하고 로그인 페이지로 리다이렉트된다")
	void signup_succeedsWithCsrfToken() throws Exception {
		mockMvc.perform(post("/auth/signup")
				.with(csrf())
				.param("username", "newuser")
				.param("email", "new@example.com")
				.param("password", "password123")
				.param("passwordConfirm", "password123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/auth/login?signup=true"));
	}

	@Test
	@DisplayName("비밀번호와 비밀번호 확인이 다르면 가입되지 않는다")
	void signup_failsWhenPasswordConfirmDiffers() throws Exception {
		mockMvc.perform(post("/auth/signup")
				.with(csrf())
				.param("username", "mismatch")
				.param("email", "mismatch@example.com")
				.param("password", "password123")
				.param("passwordConfirm", "different123"))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/signup"))
			.andExpect(model().attributeExists("error"));
	}

	@Test
	@DisplayName("올바른 자격 증명으로 로그인하면 인증 상태가 된다")
	void login_succeedsWithCorrectCredentials() throws Exception {
		userService.signup("loginuser", "login@example.com", "password123");

		mockMvc.perform(formLogin("/auth/login").user("loginuser").password("password123"))
			.andExpect(authenticated().withUsername("loginuser"));
	}

	@Test
	@DisplayName("잘못된 비밀번호로 로그인하면 인증되지 않는다")
	void login_failsWithWrongPassword() throws Exception {
		userService.signup("wrongpwuser", "wrongpw@example.com", "password123");

		mockMvc.perform(formLogin("/auth/login").user("wrongpwuser").password("WRONGPASSWORD"))
			.andExpect(unauthenticated())
			.andExpect(redirectedUrl("/auth/login?error=true"));
	}

}
