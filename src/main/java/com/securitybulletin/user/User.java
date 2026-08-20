package com.securitybulletin.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
	@Index(name = "idx_username", columnList = "username", unique = true),
	@Index(name = "idx_email", columnList = "email", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "사용자명은 필수입니다")
	@Size(min = 3, max = 50, message = "사용자명은 3~50자여야 합니다")
	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@NotBlank(message = "비밀번호는 필수입니다")
	@Size(min = 8, message = "비밀번호는 최소 8자여야 합니다")
	@Column(nullable = false, length = 100)  // BCrypt 해시는 보통 60자, 여유 두어 100자
	private String password;

	@NotBlank(message = "이메일은 필수입니다")
	@Email(message = "올바른 이메일 형식이 아닙니다")
	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
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

}
