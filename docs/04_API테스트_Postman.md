# API 테스트 및 Postman 콜렉션

**관련 문서:** [README](../README.md) · [01_기획.md §6 API 명세](01_기획.md#6-api-명세서) · [검증안내서](../검증안내서.md)

> **이 문서는 2026-08-20에 실제 구현 기준으로 다시 작성되었다.**
> 최초 설계본은 JWT Bearer 토큰 + JSON 본문 + `PUT`/`DELETE` + 201 Created를 전제했으나,
> 실제 구현은 **세션 쿠키 + 폼 전송 + CSRF 토큰 + 302 리다이렉트** 방식이다.
> 아래 내용은 `postman/` 폴더의 실제 파일과 일치하며, newman으로 실행해 **단언 39건 전부 통과**를 확인했다.

---

## 1. 파일 위치

| 파일 | 용도 |
|---|---|
| `postman/Secure_Bulletin_Board.postman_collection.json` | 콜렉션 본체 (요청 25개, 단언 39개) |
| `postman/Local.postman_environment.json` | 로컬 환경 — `baseUrl = http://localhost:8080/api` |
| `postman/Production.postman_environment.json` | 배포 환경 — 배포 시 호스트만 교체 |

### Postman 클라우드

같은 콜렉션이 개인 워크스페이스 **My Workspace**에도 올라가 있다 (2026-08-20).

| 항목 | ID |
|---|---|
| 워크스페이스 | `3db457d9-e8ce-4c62-88d8-62324b860269` (visibility: personal) |
| 콜렉션 | `30578340-a3505bfc-fb6c-4942-bf90-8c016a8a7735` |
| 환경 — Local | `30578340-9a518974-15fd-45d1-b5dc-c992ecb7c18b` |
| 환경 — Production | `30578340-f3f0be7d-bc1a-4c94-a15a-dc31aa93e105` |

> **클라우드 버전은 폴더 없이 평면 구조다.** Postman API의 폴더 중첩 스키마가 하위 항목에 테스트 스크립트와
> `urlencoded` 본문을 허용하지 않아, 폴더를 쓰면 단언 39개가 전부 사라진다. 번호(`0-1`, `1-1`, `2-1`, `3-1`…)로
> 그룹과 실행 순서를 표현했다. 로컬 JSON 파일은 폴더 구조를 유지하고 있으므로, 폴더가 보이는 편이 좋다면
> 클라우드 대신 **로컬 파일을 Import** 하면 된다.
>
> 두 버전은 자동으로 동기화되지 않는다. 콜렉션을 고쳤다면 양쪽 모두 반영할 것.

---

## 2. 이 API의 특성 (콜렉션을 이해하는 데 필요한 전제)

이 프로젝트는 REST/JWT API가 아니라 **서버 렌더링 + 세션 인증** 방식이다. 그래서 일반적인 Postman 콜렉션과 다른 점이 세 가지 있다.

### 2-1. 인증은 세션 쿠키로 유지된다

로그인에 성공하면 `JSESSIONID` 쿠키가 발급되고, 이후 요청은 이 쿠키로 인증된다.
Postman과 newman은 쿠키 저장소를 자동으로 관리하므로 `Authorization` 헤더를 직접 넣을 필요가 없다.

대신 **요청 순서가 중요하다.** 쿠키 저장소가 하나뿐이므로 bob으로 로그인하면 alice 세션은 대체된다.
콜렉션은 이 성질을 이용해 "alice가 쓴 글을 bob이 건드려본다"는 인가 시나리오를 구성한다.

### 2-2. 상태 변경 요청에는 CSRF 토큰이 필요하다

CSRF 보호가 켜져 있으므로 모든 POST에 `_csrf` 값이 있어야 한다. 토큰은 세션에 묶여 있고, 화면에서는 Thymeleaf가 hidden 필드로 자동 삽입한다.

콜렉션은 **콜렉션 수준 Pre-request Script**로 이 과정을 자동화한다.

```javascript
const raw = pm.request.body ? JSON.stringify(pm.request.body) : '';
if (pm.request.method === 'POST' && raw.indexOf('csrfToken') !== -1) {
    pm.sendRequest({
        url: pm.collectionVariables.get('baseUrl') + '/auth/login',
        method: 'GET'
    }, function (err, res) {
        if (err) { console.log('CSRF 발급 실패: ' + err); return; }
        const m = res.text().match(/name="_csrf"\s+value="([^"]+)"/);
        if (m) { pm.collectionVariables.set('csrfToken', m[1]); }
    });
}
```

> **핵심**: 요청 본문에 `_csrf` 필드가 있는 요청만 토큰을 채운다.
> CSRF 방어를 테스트하는 요청(3-1)은 일부러 `_csrf` 필드를 두지 않았고, 그래서 스크립트가 건너뛰어 토큰 없이 전송된다.
> 이 조건을 없애면 CSRF 테스트가 조용히 무의미해진다.

### 2-3. 응답이 JSON이 아니라 302 또는 HTML이다

성공은 대부분 302 리다이렉트고, 실패(검증 오류)는 200 + HTML이다.
Postman은 기본적으로 리다이렉트를 자동으로 따라가므로 302를 검증할 수 없다. 그래서 상태 변경 요청마다 다음 설정을 넣었다.

```json
"protocolProfileBehavior": { "followRedirects": false }
```

검증은 **상태 코드 + `Location` 헤더 + 본문 문자열**로 한다.

---

## 3. 콜렉션 구조

```
Secure Bulletin Board API
├─ 00. 준비
│  └─ 0-1. 서버 확인 및 실행 ID 생성       ← 사용자명 충돌 방지용 접미사 생성
│
├─ 01. 인증 (Auth)
│  ├─ 1-1. 회원가입 — alice                → 302 /auth/login?signup=true
│  ├─ 1-2. 회원가입 — bob                  → 302
│  ├─ 1-3. 회원가입 실패 — 중복 사용자명    → 200 + "이미 사용 중인 사용자명"
│  ├─ 1-4. 로그인 실패 — 틀린 비밀번호      → 302 ?error=true
│  ├─ 1-5. 로그인 — alice                  → 302 /posts + JSESSIONID 발급
│  └─ 1-6. 로그인 상태 확인 (/auth/me)      → 200 {"username": "..."}
│
├─ 02. 게시글 (Posts)
│  ├─ 2-1. 글 작성                         → 302 /posts/{id}, postId 저장
│  ├─ 2-2. 글 상세 조회                    → 200 + 제목 확인
│  ├─ 2-3. 글 목록 조회                    → 200 + 목록에 노출 확인
│  ├─ 2-4. 글 작성 실패 — 제목 없음         → 200 + "제목은 필수입니다"
│  ├─ 2-5. 본인 글 수정                    → 302
│  └─ 2-6. 수정 반영 확인                  → 200 + 수정된 제목
│
└─ 03. 보안 테스트 (Penetration Tests)
   ├─ 3-1. CSRF — 토큰 없이 글 작성         → 403
   ├─ 3-2. XSS — 스크립트 페이로드 저장     → 302 (저장은 성공)
   ├─ 3-3. XSS — 출력 이스케이프 확인       → 원본 태그 없음, &lt;script&gt; 존재
   ├─ 3-4. SQL Injection — 로그인 우회 시도 → 302 ?error=true, 5xx 아님
   ├─ 3-5. 인가 — bob으로 로그인            → 세션 교체
   ├─ 3-6. 인가 — 타인 글 수정 시도         → 403
   ├─ 3-7. 인가 — 타인 글 삭제 시도         → 403
   ├─ 3-8. 인가 — 실제로 안 바뀌었는지 확인 → 원래 제목 유지, "탈취" 문자열 없음
   ├─ 3-9. 존재하지 않는 글 조회            → 404
   ├─ 3-10. 세션 — 로그아웃                → 302 ?logout=true
   ├─ 3-11. 세션 — 로그아웃 후 접근         → 302 /auth/login
   └─ 3-12. 세션 — 비로그인 작성 시도       → 302, 글 생성 안 됨
```

### 3-8이 따로 있는 이유

403을 받았다는 사실만으로는 "막혔다"고 할 수 없다. 서버가 403을 돌려주면서 데이터는 바꿔놓았을 수도 있기 때문이다.
그래서 인가 테스트는 **거부 응답 확인 + 데이터 불변 확인**을 짝으로 둔다. 이 원칙은 `PostControllerTest`의 인가 테스트에도 똑같이 적용돼 있다.

---

## 4. 실행 방법

### 4-1. Postman UI

1. Postman → Import → `postman/` 폴더의 JSON 3개를 모두 가져온다
2. 오른쪽 위 환경 선택에서 **Local** 선택
3. 백엔드를 띄운다: `./gradlew bootRun`
4. 콜렉션 우클릭 → **Run collection** → Run

> 요청을 개별 실행하지 말고 **순서대로 전체 실행**해야 한다. 앞 요청이 뒤 요청의 변수(`postId` 등)와 세션을 만든다.

### 4-2. newman (CLI)

```bash
npx newman run "postman/Secure_Bulletin_Board.postman_collection.json" -e "postman/Local.postman_environment.json"
```

HTML 리포트까지 남기려면:

```bash
npx newman run "postman/Secure_Bulletin_Board.postman_collection.json" -e "postman/Local.postman_environment.json" -r cli,htmlextra --reporter-htmlextra-export postman/results/report.html
```

`postman/results/`는 `.gitignore`에 등록되어 있다.

### 4-3. 실행 결과 (2026-08-20)

```
                         executed    failed
        iterations              1         0
          requests             40         0
      test-scripts             25         0
prerequest-scripts             25         0
        assertions             39         0
```

> requests가 40인 것은 CSRF 토큰을 받아오는 사전 요청이 함께 계수되기 때문이다.

---

## 5. 반복 실행이 가능한 이유

H2가 인메모리라 서버를 재시작하면 데이터가 사라지지만, **서버를 켠 채로 두 번 실행하면** 같은 사용자명으로 다시 가입하려다 실패한다.
그래서 `0-1` 요청이 실행마다 타임스탬프 기반 접미사를 만들어 `alice123456` 같은 이름을 쓴다.

```javascript
const runId = String(Date.now()).slice(-6);
pm.collectionVariables.set('aliceUser', 'alice' + runId);
pm.collectionVariables.set('bobUser', 'bob' + runId);
```

덕분에 서버를 재시작하지 않고도 콜렉션을 몇 번이든 다시 돌릴 수 있다.

---

## 6. 다른 테스트와의 역할 구분

| 테스트 | 도구 | 무엇을 잡는가 |
|---|---|---|
| 단위·통합 테스트 (36건) | JUnit 5, MockMvc | 코드 수준의 로직·인가·검증. 리팩터링 시 회귀 방지 |
| **API·보안 테스트 (39건)** | **Postman / newman** | **실제 HTTP 클라이언트 시점.** 세션 쿠키, CSRF 토큰 발급, 리다이렉트 체인처럼 MockMvc가 흉내만 내는 부분 |
| 침투 스캔 | OWASP ZAP | 알려진 취약점 패턴 자동 탐색 (4주차 예정) |

MockMvc는 서블릿 컨테이너를 실제로 띄우지 않으므로, 쿠키 전달이나 리다이렉트 추적이 실제 브라우저와 완전히 같지는 않다.
Postman 콜렉션은 그 간극을 메우는 역할이다 — 실제로 3-12(비로그인 작성)처럼 **실행 중인 서버에서만 확인되는 동작**이 있다.

---

## 7. CI 통합 (배포 시)

```yaml
# .github/workflows/api-test.yml
name: API Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: 백엔드 기동
        run: ./gradlew bootRun &
      - name: 기동 대기
        run: npx wait-on http://localhost:8080/api/posts --timeout 120000
      - name: 콜렉션 실행
        run: npx newman run postman/Secure_Bulletin_Board.postman_collection.json -e postman/Local.postman_environment.json
```

배포 후에는 `-e postman/Production.postman_environment.json`으로 같은 콜렉션을 운영 환경에 그대로 돌린다.
