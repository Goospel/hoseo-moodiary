# 발표 자료

졸업 발표용 슬라이드 모음. **Marp** (markdown → 슬라이드) 포맷.

## 파일

| 파일 | 용도 |
|---|---|
| [`moodiary-be-presentation.md`](./moodiary-be-presentation.md) | 백엔드 파트 메인 발표 슬라이드 (2026-05-27 갱신 — PR 4-pre / PR 10 반영) |

## 사용 방법 — 세 가지 길

### 1️⃣ VS Code 확장으로 즉시 미리보기 (가장 쉬움)

1. VS Code 에 **Marp for VS Code** 확장 설치 (`marp-team.marp-vscode`)
2. `moodiary-be-presentation.md` 열기
3. 오른쪽 상단의 미리보기 버튼 (📄 옆 화살표) → "Open Side Preview"
4. **발표자 모드** — `View > Command Palette` → "Marp: Open Presenter View"
   → 슬라이드 + **대본** + 다음 슬라이드 + 타이머가 함께 표시됨
5. 또는 export 버튼으로 PPTX/PDF/HTML 출력 (대본은 PPTX 의 speaker note 로 자동 변환)

### 2️⃣ CLI 로 변환 (PPTX / PDF / HTML)

```bash
# 한 번 설치 불필요 — npx 로 즉시
npx @marp-team/marp-cli@latest moodiary-be-presentation.md --pptx
npx @marp-team/marp-cli@latest moodiary-be-presentation.md --pdf
npx @marp-team/marp-cli@latest moodiary-be-presentation.md --html

# 또는 글로벌 설치
npm install -g @marp-team/marp-cli
marp moodiary-be-presentation.md --pptx
```

→ 같은 폴더에 `moodiary-be-presentation.pptx` / `.pdf` / `.html` 생성

> **PPTX export 시 대본은 자동으로 PowerPoint 의 "슬라이드 노트" 영역에 들어간다** — 발표자 모드에서 그대로 보임.

### 3️⃣ 웹에서 바로 (설치 없이)

[https://web.marp.app](https://web.marp.app) 에서 markdown 붙여넣기 → 미리보기 + 다운로드

## 발표 구성 (5장 섹션 + 표지/감사 = **약 28장**)

1. **프로젝트 한눈에** (3장) — Moodiary 가 무엇인가, 3인 분업, 스택과 운영
2. **여정** (4장) — 6단계 진행 흐름 + 단계별 핵심 결정 (1-2 / 3 / 4-5 / 6)
3. **핵심 구현** (10장) ⭐ — README 강조 4영역 그대로
    - **3-1 아키텍처** (3장) — 레이어/패키지 + 인증 흐름 + 비동기 AI 흐름
    - **3-2 데이터베이스** (2장) — ER 다이어그램 + 핵심 정책
    - **3-3 인프라** (2장) — Runtime + CI/CD 파이프라인
    - **3-4 보안** (2장) — 인증 + 인가/CORS/시크릿
    - **3-5 API 표면** (1장) — 운영 반영된 모든 endpoint
    - **3-6 테스트 전략** (1장)
4. **핵심 트러블슈팅** (6장) — T-019 대사건 + T-023 silent fail + T-015→T-018→T-020 워크플로우 학습
5. **앞으로** (3장) — 외부 합의 대기 / 단독 진행 가능 / 먼 미래

## 대본 (Speaker Notes)

각 슬라이드 끝에 **HTML 주석 (`<!-- ... -->`)** 으로 대본이 박혀 있다.

- **VS Code Marp** — 발표자 모드에서 자동 노출
- **PPTX export** — PowerPoint 의 슬라이드 노트로 자동 변환
- **HTML export** — `?presenter` 쿼리 파라미터로 발표자 뷰 활성화

대본 길이는 슬라이드당 약 30초 ~ 1분 30초 분량. 슬라이드 본문을 그대로 읽는 게 아니라 추가 맥락 / 결정 이유 / 청중 입장에서 이해할 풀이를 담음.

## 편집 팁

- 슬라이드 구분은 `---` (수평선)
- 표지 / 섹션 도입 슬라이드는 `<!-- _class: lead -->` 으로 가운데 정렬
- **대본은 슬라이드 본문 다음의 일반 HTML 주석** — Marp 가 presenter note 로 자동 인식
- 스타일 (색상/폰트) 은 파일 상단의 `style:` 블록에서 조정
- 코드 블록 / 표는 일반 markdown 그대로

## 발표 시간 가이드

- **총 약 20-25분 분량** (대본 기준)
- 슬라이드당 평균 45초 — 핵심 구현 섹션이 가장 풍부
- 시간 부족하면:
  - **5분 컷**: 표지 → 한눈에 → API 표면 → T-019 → 마치며
  - **10분 컷**: 5분 컷 + 아키텍처/DB/보안 슬라이드 + T-023 + 앞으로
  - **15분 컷**: 10분 컷 + 인프라 + Refresh Token 흐름 + 워크플로우 학습
- 트러블슈팅 (4번 섹션) 이 가장 풍부 — 학습 강조 포인트

## 갱신 이력

| 일자 | 갱신 |
|---|---|
| 2026-05-27 | **PR 4-pre (비동기 AI) + PR 10 (Refresh Token) 반영** + README 의 아키텍처/DB/인프라/보안 4영역 강조 구조 반영 + **각 슬라이드에 발표 대본 (presenter note) 추가** + HS512 → HS256 교정 + PR / T / 테스트 수 갱신 |
| 2026-05-26 | 초안 작성 — 5단계 여정 + T-019/T-023 트러블슈팅 |
