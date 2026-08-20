/*
 * 화면 공통 스크립트.
 *
 * 인라인 이벤트 핸들러(onsubmit="...", onclick="...")를 쓰지 않는 이유:
 * CSP가 script-src 'self' 이므로 인라인 핸들러는 브라우저가 실행을 거부한다.
 * 이때 오류가 눈에 띄지 않고 "핸들러가 없는 것처럼" 조용히 넘어가므로,
 * 삭제 확인창 같은 장치가 사라진 줄도 모르고 배포될 수 있다. (실제로 한 번 겪었다)
 *
 * 그래서 동작은 전부 이 파일에 두고, HTML은 data-* 속성으로 의도만 표시한다.
 */
document.addEventListener('DOMContentLoaded', function () {
	// data-confirm 속성이 있는 폼은 제출 전에 사용자 확인을 받는다.
	document.querySelectorAll('form[data-confirm]').forEach(function (form) {
		form.addEventListener('submit', function (event) {
			if (!window.confirm(form.dataset.confirm)) {
				event.preventDefault();
			}
		});
	});
});
