package com.securitybulletin.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

	/**
	 * 목록 조회 (페이징).
	 * 작성자를 함께 조회해 목록에서 N+1 쿼리가 발생하지 않도록 한다.
	 *
	 * 정렬 조건은 Pageable로 받는다 — 문자열로 조립하지 않으므로 SQL Injection 경로가 생기지 않는다.
	 */
	@Override
	@EntityGraph(attributePaths = "author")
	Page<Post> findAll(Pageable pageable);

	/** 상세 조회. 작성자를 함께 로딩한다. */
	@Override
	@EntityGraph(attributePaths = "author")
	Optional<Post> findById(Long id);

}
