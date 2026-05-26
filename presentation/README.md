# 발표 자료

졸업 발표용 슬라이드 모음. **Marp** (markdown → 슬라이드) 포맷.

## 파일

| 파일 | 용도 |
|---|---|
| [`moodiary-be-presentation.md`](./moodiary-be-presentation.md) | 백엔드 파트 메인 발표 슬라이드 (2026-05-26 시점) |

## 사용 방법 — 세 가지 길

### 1️⃣ VS Code 확장으로 즉시 미리보기 (가장 쉬움)

1. VS Code 에 **Marp for VS Code** 확장 설치 (`marp-team.marp-vscode`)
2. `moodiary-be-presentation.md` 열기
3. 오른쪽 상단의 미리보기 버튼 (📄 옆 화살표) → "Open Side Preview"
4. 발표 시 슬라이드뷰로 바로 사용 가능 — 또는 export 버튼으로 PPTX/PDF/HTML 출력

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

### 3️⃣ 웹에서 바로 (설치 없이)

[https://web.marp.app](https://web.marp.app) 에서 markdown 붙여넣기 → 미리보기 + 다운로드

## 발표 구성 (5장 섹션 + 표지/감사 = 약 18장)

1. **프로젝트 한눈에** — Moodiary 가 무엇인가, 스택, 운영
2. **여정** — 5단계 진행 과정 + 단계별 핵심 결정
3. **핵심 구현** — API 표면, 보안, 인프라/CI/CD, 테스트 전략
4. **핵심 트러블슈팅** — T-019 대사건 + T-023 silent fail 진실 + 워크플로우 학습
5. **앞으로** — PR 8 마무리, PR 4 (AI 비동기), PR 6 (Flyway), 마치며

## 편집 팁

- 슬라이드 구분은 `---` (수평선)
- 표지 / 섹션 도입 슬라이드는 `<!-- _class: lead -->` 으로 가운데 정렬
- 스타일 (색상/폰트) 은 파일 상단의 `style:` 블록에서 조정
- 코드 블록 / 표는 일반 markdown 그대로

## 발표 시간 가이드

- 약 15-20분 분량으로 작성
- 트러블슈팅 (4번 섹션) 이 가장 풍부 — 학습 강조 포인트
- 시간 부족하면 4번 섹션의 "옛 CD silent fail" 부분 (T-023) 만이라도 살리기
