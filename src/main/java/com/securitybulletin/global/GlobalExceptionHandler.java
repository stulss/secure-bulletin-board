package com.securitybulletin.global;

import com.securitybulletin.post.PostNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

	/** 존재하지 않는 게시글 → 404 */
	@ExceptionHandler(PostNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String handleNotFound(PostNotFoundException e, Model model) {
		model.addAttribute("status", 404);
		model.addAttribute("message", e.getMessage());
		return "error/error";
	}

	/**
	 * 본인 글이 아닌데 수정·삭제를 시도 → 403.
	 *
	 * 이 예외는 Service 계층의 소유자 검증에서 올라온다.
	 * 화면에서 버튼을 숨기든 말든, URL을 직접 호출하면 여기로 떨어진다.
	 */
	@ExceptionHandler(AccessDeniedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public String handleAccessDenied(AccessDeniedException e, Model model) {
		model.addAttribute("status", 403);
		model.addAttribute("message", "본인 게시글만 수정·삭제할 수 있습니다.");
		return "error/error";
	}

}
