package com.securitybulletin.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 글 작성·수정 폼 입력값 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostForm {

	@NotBlank(message = "제목은 필수입니다")
	@Size(max = 200, message = "제목은 200자를 넘을 수 없습니다")
	private String title;

	@NotBlank(message = "본문은 필수입니다")
	private String content;

	public static PostForm from(Post post) {
		return new PostForm(post.getTitle(), post.getContent());
	}

}
