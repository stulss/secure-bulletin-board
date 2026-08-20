package com.securitybulletin.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTest {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("회원가입하면 비밀번호가 평문이 아닌 BCrypt 해시로 저장된다")
	void signup_storesBcryptHash_notPlaintext() {
		String rawPassword = "securepass123";

		userService.signup("bcryptuser", "bcrypt@example.com", rawPassword);

		User saved = userRepository.findByUsername("bcryptuser").orElseThrow();

		// 1) 평문이 그대로 저장되지 않았다
		assertThat(saved.getPassword()).isNotEqualTo(rawPassword);
		// 2) BCrypt 해시 형식이다 ($2a$/$2b$/$2y$ 로 시작하는 60자)
		assertThat(saved.getPassword()).matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
		// 3) 그럼에도 원래 비밀번호로는 검증이 통과한다
		assertThat(passwordEncoder.matches(rawPassword, saved.getPassword())).isTrue();
		// 4) 다른 비밀번호로는 통과하지 않는다
		assertThat(passwordEncoder.matches("wrongpassword", saved.getPassword())).isFalse();
	}

	@Test
	@DisplayName("같은 비밀번호라도 사용자마다 다른 해시가 저장된다 (salt 적용)")
	void signup_usesDifferentSaltPerUser() {
		String rawPassword = "samepassword123";

		userService.signup("saltuser1", "salt1@example.com", rawPassword);
		userService.signup("saltuser2", "salt2@example.com", rawPassword);

		String hash1 = userRepository.findByUsername("saltuser1").orElseThrow().getPassword();
		String hash2 = userRepository.findByUsername("saltuser2").orElseThrow().getPassword();

		assertThat(hash1).isNotEqualTo(hash2);
	}

	@Test
	@DisplayName("중복된 사용자명으로는 가입할 수 없다")
	void signup_rejectsDuplicateUsername() {
		userService.signup("dupuser", "dup1@example.com", "password123");

		assertThatThrownBy(() -> userService.signup("dupuser", "dup2@example.com", "password123"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("사용자명");
	}

	@Test
	@DisplayName("중복된 이메일로는 가입할 수 없다")
	void signup_rejectsDuplicateEmail() {
		userService.signup("emailuser1", "same@example.com", "password123");

		assertThatThrownBy(() -> userService.signup("emailuser2", "same@example.com", "password123"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("이메일");
	}

}
