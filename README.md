# Spring Security 기반 보안 게시판 (Secure Bulletin Board)

회원가입·로그인·CRUD가 모두 포함된 기본 게시판에 비밀번호 암호화(BCrypt)와
XSS/SQL Injection 방어를 적용한 보안 강화 웹 서비스입니다.

> 국비지원 교육 과정 수료 후 취업 포트폴리오 + Spring Security 인증/인가 실습을 목적으로 기획했습니다.
> **현재 상태: 4주차 범위까지 완료 — 로컬 실행 기준으로 완성. JUnit 36건 · Postman 39개 단언 · OWASP ZAP High 0 통과.**

## 핵심 개념

- **인가는 서버에서, 이중으로**: "본인 글만 수정/삭제 가능"을 프론트엔드 버튼 숨김이 아니라 Service 계층에서 리소스 소유자를 직접 비교해 검증합니다. 다른 사용자의 글 ID를 URL에 직접 넣어도 403으로 거부됩니다.
- **평문 비밀번호 0개**: `BCryptPasswordEncoder`로 해싱해 저장하며, DB 컬럼을 직접 열어 평문이 아님을 검증합니다.
- **입력값은 항상 의심**: 게시글 본문의 `<script>` 태그는 Thymeleaf 자동 이스케이프로 무해화되고, 검색/조회는 JPA 파라미터 바인딩만 사용해 문자열 결합 쿼리를 만들지 않습니다.

## 기술 스택

| 영역 | 기술 |
|---|---|
| Backend | Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Frontend | Thymeleaf + Bootstrap 5 (서버 렌더링, 로컬 서빙) |
| Database | H2 (로컬 개발), Supabase PostgreSQL (배포) |
| Build | Gradle 9.5.1 (Wrapper 포함) |
| Test | JUnit 5, Spring Security Test, `@SpringBootTest`, Postman/newman, OWASP ZAP 2.17 |

## 폴더 구조

```
secure-bulletin-board/                Spring Boot 프로젝트 루트
├─ Dockerfile · .dockerignore         Render 배포용 컨테이너 이미지 정의
├─ build.gradle · settings.gradle     Gradle 빌드 설정
├─ gradlew · gradlew.bat · gradle/    Gradle Wrapper (9.5.1)
├─ src/
│  ├─ main/java/com/securitybulletin/
│  │  ├─ SecureBulletinBoardApplication.java
│  │  ├─ config/SecurityConfig.java   Spring Security 필터 체인·BCrypt 인코더
│  │  ├─ global/GlobalExceptionHandler.java  404·403 처리
│  │  ├─ user/                        User 엔티티·Repository·Service·Controller·DTO
│  │  └─ post/                        Post 엔티티·Repository·Service(소유자 검증)·Controller·Form
│  └─ main/resources/
│     ├─ application.yml              H2·JPA·Thymeleaf·세션 쿠키 설정 (로컬)
│     ├─ application-prod.yml         운영 프로파일 — DB·쿠키·로그 설정 (배포)
│     ├─ db/schema-postgres.sql       Supabase 초기 스키마 (Hibernate 생성 DDL)
│     ├─ static/css/                  bootstrap.min.css · app.css (CDN 미사용)
│     └─ templates/
│        ├─ auth/                     login.html · signup.html
│        ├─ post/                     list.html · detail.html · form.html
│        └─ error/error.html          404·403 화면
├─ src/test/java/com/securitybulletin/
│  ├─ SecurityDefenseTest.java        SQLi·XSS·메서드보안·BCrypt 비가역성 (5건)
│  ├─ user/UserServiceTest.java       BCrypt 해시 저장·salt·중복 가입 (4건)
│  ├─ user/AuthControllerTest.java    CSRF·인증·로그인 실패 (6건)
│  ├─ post/PostServiceTest.java       소유자 검증·페이징·404 (8건)
│  └─ post/PostControllerTest.java    인가 403·CSRF·XSS 이스케이프·검증 (13건)
├─ postman/                           Postman 콜렉션 + 환경 파일 (newman 실행 가능)
├─ security/zap-report.html           OWASP ZAP 스캔 보고서
├─ README.md                 이 파일 — 개요·문서 색인
├─ CLAUDE.md                 AI 세션 시작 시 자동 로드되는 작업 규칙
├─ 작업내역_체크리스트.md      AI 공용 인수인계 파일 (진행 상황·결정 기록·작업 로그)
├─ 검증안내서.md              어디로 가서 무엇을 하면 무엇이 보여야 통과인지
├─ 트러블슈팅.md              실제로 겪은 문제·시도·비교·배운 점
├─ AI_3줄.md                 AI에게 맡긴 일 / 내가 판단한 일 / AI 말을 안 들은 일
├─ 포트폴리오_추가용_소개글.md  포트폴리오에 이 프로젝트를 추가할 때 쓰는 소개글
└─ docs/
   ├─ 01_기획.md              개발기획서 전체 (12섹션)
   ├─ 02_UI_UX설계.md         화면별 명세·와이어프레임·공통 컴포넌트·접근성
   ├─ 03_백엔드구조.md         패키지 구조·계층 의존 규칙·인증/인가 흐름·예외 처리
   ├─ 04_API테스트_Postman.md  Postman 콜렉션·환경변수·침투 테스트·Newman
   └─ 05_배포.md              배포 절차 — Render(앱) + Supabase(DB)
```

## 실행 방법

**백엔드** (http://localhost:8080/api)

```bash
./gradlew bootRun
```

화면까지 서버가 렌더링하므로 별도 프론트엔드 서버가 필요 없습니다.

**테스트** — JUnit 36건

```bash
./gradlew test
```

**API·보안 테스트** — Postman 콜렉션 39개 단언 (백엔드가 떠 있어야 함)

```bash
npx newman run postman/Secure_Bulletin_Board.postman_collection.json -e postman/Local.postman_environment.json
```

> 사전 요구사항: JDK 17 (Temurin 17 설치 확인됨). Gradle은 Wrapper가 포함되어 별도 설치가 필요 없습니다.
> Postman 콜렉션을 CLI로 돌릴 때만 Node.js가 필요합니다(`npx newman`).

## 문서

| 문서 | 내용 |
|---|---|
| [작업내역_체크리스트.md](작업내역_체크리스트.md) | 진행 상황·결정 기록·작업 로그 (여러 AI 모델이 이어서 작업하는 규칙 포함) |
| [검증안내서.md](검증안내서.md) | 어디로 가서 무엇을 하면 무엇이 보여야 통과인지 |
| [트러블슈팅.md](트러블슈팅.md) | 실제로 겪은 문제의 문제·시도·비교·배운 점 |
| [AI_3줄.md](AI_3줄.md) | AI에게 맡긴 일 / 내가 판단한 일 / AI 말을 안 들은 일 |
| [포트폴리오_추가용_소개글.md](포트폴리오_추가용_소개글.md) | 개인 포트폴리오에 이 프로젝트를 추가할 때 쓰는 소개글 |
| [docs/01_기획.md](docs/01_기획.md) | **개발기획서** — 배경·타겟사용자·기능명세·아키텍처·ERD·API명세·보안요구사항·개발일정·테스트계획·리스크·포트폴리오포인트·확장계획 |
| [docs/02_UI_UX설계.md](docs/02_UI_UX설계.md) | **UI/UX 설계** — 사이트맵·화면별 명세(목록/상세/작성/수정/로그인/회원가입)·공통 컴포넌트·접근성 |
| [docs/03_백엔드구조.md](docs/03_백엔드구조.md) | **백엔드 구조** — 패키지 설계·계층 의존 규칙·인증/인가가 실제로 흐르는 경로·예외 처리 전략 |
| [docs/04_API테스트_Postman.md](docs/04_API테스트_Postman.md) | **API 테스트 및 Postman 콜렉션** — 환경변수·테스트 스크립트·보안 침투 테스트 시나리오·Newman 자동화·CI/CD 통합 |
| [docs/05_배포.md](docs/05_배포.md) | **배포 절차** — Render(앱) + Supabase(DB), 무료 호스트 비교와 검증 결과 |
| [다이어그램.canvas](다이어그램.canvas) | Obsidian Canvas — 문서 맵·시스템 아키텍처(MVC)·ERD·4주 로드맵을 한 화면에서 시각화 (Obsidian에서 열어 확인) |

## 현재 상태

| 항목 | 상태 |
|---|---|
| 기획 | ✅ 완료 — [docs/01_기획.md](docs/01_기획.md) |
| UI/UX·백엔드·API 테스트 설계 | ✅ 완료 — [docs/02_UI_UX설계.md](docs/02_UI_UX설계.md) |
| 프로젝트 뼈대(Gradle/Spring Boot) | ✅ 완료 — 빌드 성공 |
| 회원가입/로그인 | ✅ 완료 — 브라우저에서 가입→로그인→세션 인증 확인 |
| 게시글 CRUD | ✅ 완료 — 목록(페이징)·상세·작성·수정·삭제 |
| Spring Security 통합(인가) | ✅ 완료 — `@PreAuthorize` + Service 소유자 검증(403), 로그아웃, 세션 고정 방지 |
| 보안 강화(XSS/CSRF/BCrypt/SQLi 검증) | ✅ 완료 — 4가지 방어 모두 테스트로 증명 |
| 테스트 | ✅ JUnit 36건 + Postman 39개 단언 전부 통과 |
| OWASP ZAP 스캔 | ✅ **High 0 · Low 0** — 지적 3건 수정 후 재스캔 |
| 배포 | 🔶 **진행 중** — Render(앱) + Supabase(DB). Dockerfile·prod 프로파일 완료, 로컬에서 실제 Supabase 연결·회원가입·로그인 검증 완료. Render 서비스 생성은 다음 단계 |

**현재 전략**: 로컬은 `./gradlew bootRun` 하나로 완결됩니다(H2). 배포는 **Render + Supabase** 조합으로 진행 중입니다 — 신용카드 등록 없이 되는 조합을 여러 후보 중에서 골랐습니다. 자세한 비교와 절차는 [docs/05_배포.md](docs/05_배포.md).

**검증 결과** — `./gradlew test` **36건 전부 통과** + 실행 중인 서버에서 직접 확인:
- 비밀번호가 평문이 아닌 BCrypt 해시(`$2a$10$…`, 60자)로 저장되고, 같은 비밀번호도 사용자마다 다른 해시(salt)가 된다
- **다른 사용자의 글 ID를 URL에 직접 넣어 수정·삭제하면 403이고, 내용이 실제로 바뀌지 않는다** (버튼을 숨기는 것으로 끝내지 않았다)
- 본문에 `<script>alert(1)</script>`를 저장해도 브라우저 DOM에 `script` 요소가 생기지 않고 텍스트로만 렌더링된다
- CSRF 토큰 없는 POST는 403으로 거부된다
- 로그인 아이디에 `' OR '1'='1`을 넣어도 인증되지 않고, `'; DROP TABLE users; --`는 리터럴로 취급되어 테이블이 보존된다
- Controller를 거치지 않고 Service를 직접 호출해도 `@PreAuthorize`가 막는다
- 로그인 시 세션 ID가 재발급되고(세션 고정 방지), 로그아웃 후에는 보호된 페이지에 접근할 수 없다
- OWASP ZAP 스캔 결과 High·Low 0건 (지적받은 3건은 수정, 남은 1건은 근거와 함께 수용)

### 응답에 붙는 보안 헤더

```
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self';
                         img-src 'self' data:; form-action 'self';
                         frame-ancestors 'self'; base-uri 'self'; object-src 'none'
Set-Cookie: JSESSIONID=...; Path=/api; HttpOnly; SameSite=Lax
```

CSP에 `'unsafe-inline'`이 없습니다. 그래서 Bootstrap을 CDN이 아니라 `static/css/`에서 서빙하고,
템플릿에 인라인 `style="..."` 속성을 쓰지 않습니다 — 하나라도 어기면 `'unsafe-inline'`을 열어야 하고,
그 순간 CSP가 막아야 할 주입 경로가 함께 열립니다.

### 인가를 세 겹으로 두는 이유

| 계층 | 무엇을 막는가 |
|---|---|
| `SecurityConfig` URL 매처 | 비로그인 사용자의 작성·수정·삭제 요청 |
| `@PreAuthorize("isAuthenticated()")` | Controller를 우회한 Service 직접 호출 |
| `PostService`의 소유자 검증 | **로그인은 했지만 남의 글인 경우** — 프론트에서 버튼을 숨기든 말든 URL을 직접 치면 여기서 403 |

앞의 두 겹은 "로그인했는가"만 답한다. "본인 글인가"는 세 번째 겹만 답할 수 있고, 이 프로젝트가 증명하려는 것이 바로 그 지점이다.

상세 마일스톤과 체크리스트는 [작업내역_체크리스트.md](작업내역_체크리스트.md)를 참고하세요.
