package com.securitybulletin.global;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	/** 루트는 게시글 목록으로 보낸다 (docs/02_UI_UX설계.md 사이트맵 기준) */
	@GetMapping("/")
	public String home() {
		return "redirect:/posts";
	}

}
