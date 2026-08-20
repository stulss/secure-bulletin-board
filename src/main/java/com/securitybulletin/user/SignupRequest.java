package com.securitybulletin.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

	@NotBlank(message = "사용자명은 필수입니다")
	@Size(min = 3, max = 50, message = "사용자명은 3~50자여야 합니다")
	private String username;

	@NotBlank(message = "이메일은 필수입니다")
	@Email(message = "올바른 이메일 형식이 아닙니다")
	private String email;

	@NotBlank(message = "비밀번호는 필수입니다")
	@Size(min = 8, message = "비밀번호는 최소 8자여야 합니다")
	private String password;

	@NotBlank(message = "비밀번호 확인은 필수입니다")
	private String passwordConfirm;

}
