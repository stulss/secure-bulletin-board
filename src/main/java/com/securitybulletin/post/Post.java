package com.securitybulletin.post;

import com.securitybulletin.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 게시글.
 *
 * User와는 단방향 @ManyToOne 으로만 연결한다.
 * User에 @Data가 붙어 있어 양방향으로 만들면 toString/equals가 서로를 호출해 무한재귀가 난다.
 */
@Entity
@Table(name = "posts", indexes = {
	@Index(name = "idx_post_user", columnList = "user_id"),
	@Index(name = "idx_post_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 작성자. FK 제약으로 존재하지 않는 사용자의 글이 생기지 않도록 DB 레벨에서도 막는다. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User author;

	@NotBlank(message = "제목은 필수입니다")
	@Size(max = 200, message = "제목은 200자를 넘을 수 없습니다")
	@Column(nullable = false, length = 200)
	private String title;

	@NotBlank(message = "본문은 필수입니다")
	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	/** 이 글의 작성자가 주어진 사용자와 같은지. 인가 판단의 단일 기준점. */
	public boolean isAuthoredBy(Long userId) {
		return this.author != null && this.author.getId().equals(userId);
	}

}
