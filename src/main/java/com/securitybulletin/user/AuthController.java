package com.securitybulletin.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;

	/** 회원가입 페이지 */
	@GetMapping("/signup")
	public String signupPage(Model model) {
		model.addAttribute("signupRequest", new SignupRequest());
		return "auth/signup";
	}

	/** 회원가입 처리 */
	@PostMapping("/signup")
	public String signup(
		@Valid @ModelAttribute SignupRequest signupRequest,
		BindingResult bindingResult,
		Model model
	) {
		if (bindingResult.hasErrors()) {
			return "auth/signup";
		}

		if (!signupRequest.getPassword().equals(signupRequest.getPasswordConfirm())) {
			model.addAttribute("error", "비밀번호가 일치하지 않습니다");
			return "auth/signup";
		}

		try {
			userService.signup(
				signupRequest.getUsername(),
				signupRequest.getEmail(),
				signupRequest.getPassword()
			);
			// 리다이렉트 시 model 속성은 유지되지 않으므로 쿼리 파라미터로 성공을 알린다
			return "redirect:/auth/login?signup=true";

		} catch (IllegalArgumentException e) {
			model.addAttribute("error", e.getMessage());
			return "auth/signup";
		}
	}

	/**
	 * 로그인 페이지.
	 * 실제 로그인 처리(POST /auth/login)는 Spring Security 필터가 가로채므로
	 * 컨트롤러에 POST 핸들러를 두지 않는다.
	 */
	@GetMapping("/login")
	public String loginPage() {
		return "auth/login";
	}

	/** 현재 로그인한 사용자 정보 (인증 확인용) */
	@GetMapping("/me")
	@ResponseBody
	public ResponseEntity<Map<String, String>> currentUser(@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}
		return ResponseEntity.ok(Map.of("username", principal.getUsername()));
	}

}
