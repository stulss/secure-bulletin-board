package com.securitybulletin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // @PreAuthorize 활성화 (Service 계층 메서드 보안)
public class SecurityConfig {

	/**
	 * BCrypt 비밀번호 인코더 (10 라운드)
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(10);
	}

	/**
	 * Spring Security 필터 체인 설정
	 *
	 * 아래 경로는 context-path 를 제외한 값이다. 지금은 context-path 가 없으므로
	 * 실제 URL 과 그대로 일치한다. (예: /auth/login)
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			// CSRF 보호 활성화 — Thymeleaf 폼에 _csrf 토큰이 자동 삽입된다.
			// H2 콘솔은 자체 폼에 토큰을 넣지 못하므로 그 경로만 예외 처리 (개발 전용).
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/h2-console/**"))

			.formLogin(form -> form
				.loginPage("/auth/login")
				.loginProcessingUrl("/auth/login")
				.usernameParameter("username")
				.passwordParameter("password")
				.defaultSuccessUrl("/posts", true)
				.failureUrl("/auth/login?error=true")
				.permitAll())

			.logout(logout -> logout
				.logoutUrl("/auth/logout")
				.logoutSuccessUrl("/auth/login?logout=true")
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID")
				.permitAll())

			// 세션 고정 공격 방지: 로그인 시 세션 ID를 새로 발급하고 기존 속성만 옮긴다
			.sessionManagement(session -> session
				.sessionFixation(fixation -> fixation.migrateSession()))

			.authorizeHttpRequests(authz -> authz
				.requestMatchers("/auth/signup", "/auth/login").permitAll()
				.requestMatchers("/h2-console/**").permitAll()
				.requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
				// 글 목록·상세는 비로그인도 열람 가능.
				// GET만 열어두므로 작성(POST /posts)·수정·삭제는 아래 authenticated()에 걸린다.
				.requestMatchers(HttpMethod.GET, "/", "/posts", "/posts/{id:[0-9]+}").permitAll()
				.anyRequest().authenticated())

			.headers(headers -> headers
				// H2 콘솔이 iframe으로 렌더링되므로 동일 출처 허용 (개발 전용)
				.frameOptions(frame -> frame.sameOrigin())

				// CSP: 스크립트·스타일을 자기 출처에서만 불러온다.
				// Bootstrap을 CDN이 아니라 /css/ 아래에 두는 이유가 이것이다 —
				// 외부 출처를 열어두면 CSP가 막아줄 수 있는 XSS 경로가 도로 열린다.
				// 'unsafe-inline'은 script-src에도 style-src에도 두지 않는다.
				// 그래서 템플릿에 인라인 style="..." 속성을 쓰지 않고 /css/app.css 로 뺐다.
				.contentSecurityPolicy(csp -> csp.policyDirectives(
					"default-src 'self'; " +
					"script-src 'self'; " +
					"style-src 'self'; " +
					"img-src 'self' data:; " +
					"form-action 'self'; " +
					"frame-ancestors 'self'; " +
					"base-uri 'self'; " +
					"object-src 'none'")));

		return http.build();
	}

}
