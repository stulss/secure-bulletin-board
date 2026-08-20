package com.securitybulletin.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 회원가입
	 * @param username 사용자명
	 * @param email 이메일
	 * @param password 평문 비밀번호
	 * @return 생성된 User
	 * @throws IllegalArgumentException 중복된 사용자명/이메일
	 */
	@Transactional
	public User signup(String username, String email, String password) {
		// 중복 검사
		if (userRepository.existsByUsername(username)) {
			throw new IllegalArgumentException("이미 사용 중인 사용자명입니다: " + username);
		}
		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + email);
		}

		// BCrypt로 비밀번호 암호화
		String hashedPassword = passwordEncoder.encode(password);

		// 사용자 생성 및 저장
		User user = User.builder()
			.username(username)
			.email(email)
			.password(hashedPassword)
			.build();

		return userRepository.save(user);
	}

	/**
	 * 사용자명으로 사용자 조회 (로그인 시 사용)
	 * @param username 사용자명
	 * @return Optional<User>
	 */
	@Transactional(readOnly = true)
	public Optional<User> findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	/**
	 * ID로 사용자 조회
	 * @param userId 사용자 ID
	 * @return Optional<User>
	 */
	@Transactional(readOnly = true)
	public Optional<User> findById(Long userId) {
		return userRepository.findById(userId);
	}

	/**
	 * 비밀번호 검증
	 * @param rawPassword 평문 비밀번호
	 * @param encodedPassword 해시된 비밀번호
	 * @return true if matches
	 */
	public boolean verifyPassword(String rawPassword, String encodedPassword) {
		return passwordEncoder.matches(rawPassword, encodedPassword);
	}

}
