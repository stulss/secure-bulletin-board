package com.securitybulletin.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {

	private Long id;
	private String username;
	private String email;
	private LocalDateTime createdAt;

	public static SignupResponse fromUser(User user) {
		return new SignupResponse(
			user.getId(),
			user.getUsername(),
			user.getEmail(),
			user.getCreatedAt()
		);
	}

}
