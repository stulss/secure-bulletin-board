package com.securitybulletin.post;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

	private static final int PAGE_SIZE = 10;

	private final PostService postService;

	/** 글 목록 (비로그인도 열람 가능) */
	@GetMapping
	public String list(@RequestParam(defaultValue = "0") int page, Model model) {
		Page<Post> posts = postService.findAll(
			PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));

		model.addAttribute("posts", posts);
		return "post/list";
	}

	/** 글 상세 (비로그인도 열람 가능) */
	@GetMapping("/{id}")
	public String detail(@PathVariable Long id,
	                     @AuthenticationPrincipal UserDetails principal,
	                     Model model) {
		Post post = postService.findById(id);
		String username = (principal == null) ? null : principal.getUsername();

		model.addAttribute("post", post);
		// 화면에서 수정/삭제 버튼을 보일지 판단하는 값일 뿐,
		// 실제 인가는 Service 계층에서 다시 검증한다.
		model.addAttribute("canEdit", postService.isAuthor(id, username));
		return "post/detail";
	}

	/** 글 작성 폼 (로그인 필요) */
	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("postForm", new PostForm());
		model.addAttribute("mode", "create");
		return "post/form";
	}

	/** 글 작성 처리 */
	@PostMapping
	public String create(@Valid @ModelAttribute PostForm postForm,
	                     BindingResult bindingResult,
	                     @AuthenticationPrincipal UserDetails principal,
	                     Model model) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("mode", "create");
			return "post/form";
		}

		Post saved = postService.create(principal.getUsername(), postForm.getTitle(), postForm.getContent());
		return "redirect:/posts/" + saved.getId();
	}

	/** 글 수정 폼 (작성자 본인만) */
	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id,
	                       @AuthenticationPrincipal UserDetails principal,
	                       Model model) {
		Post post = postService.findById(id);
		// 폼을 열기 전에도 확인하지만, 최종 판단은 update() 안의 검증이다
		postService.requireAuthorOrThrow(id, principal.getUsername());

		model.addAttribute("postForm", PostForm.from(post));
		model.addAttribute("postId", id);
		model.addAttribute("mode", "edit");
		return "post/form";
	}

	/** 글 수정 처리 (작성자 본인만) */
	@PostMapping("/{id}/edit")
	public String edit(@PathVariable Long id,
	                   @Valid @ModelAttribute PostForm postForm,
	                   BindingResult bindingResult,
	                   @AuthenticationPrincipal UserDetails principal,
	                   Model model) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("postId", id);
			model.addAttribute("mode", "edit");
			return "post/form";
		}

		postService.update(id, postForm.getTitle(), postForm.getContent(), principal.getUsername());
		return "redirect:/posts/" + id;
	}

	/** 글 삭제 (작성자 본인만) */
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
		postService.delete(id, principal.getUsername());
		return "redirect:/posts";
	}

}
