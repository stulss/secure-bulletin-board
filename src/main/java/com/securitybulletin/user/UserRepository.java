package com.securitybulletin.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * 사용자명으로 사용자 조회 (로그인 시 사용)
	 * @param username 사용자명
	 * @return Optional<User>
	 */
	Optional<User> findByUsername(String username);

	/**
	 * 이메일로 사용자 존재 여부 확인
	 * @param email 이메일
	 * @return true if exists
	 */
	boolean existsByEmail(String email);

	/**
	 * 사용자명으로 사용자 존재 여부 확인
	 * @param username 사용자명
	 * @return true if exists
	 */
	boolean existsByUsername(String username);

}
