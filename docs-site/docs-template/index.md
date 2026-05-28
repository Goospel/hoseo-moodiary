# Moodiary Backend — Docs

> 호서대 컴퓨터공학부 졸업프로젝트 / 일기 + AI 응답 플랫폼 / Spring Boot 4.x REST API.
>
> **이 사이트** = `claude-docs/*` + `README.md` 를 한 곳에서 읽기 좋게 모은 곳. 검색 ⌘K.

## 📍 빠른 진입

| 가고 싶은 곳 | 어디로 |
|---|---|
| **현재 위치 / 다음 경로** | [로드맵](plan.md) |
| **API 호출 방법** | [API 명세](api-contracts.md) |
| **보안 정책 / 약점** | [보안](security.md) |
| **막혔던 곳 + 해결** | [트러블슈팅 (T-001 ~ T-028)](troubleshooting.md) |
| **모르고 물어봐서 배운 것들** | [학습 노트 (12 항목)](learning-notes.md) |
| **EC2 / FE S3 운영 가이드** | [운영 Runbook](ops-runbooks/ec2-cheatsheet.md) |
| **프로젝트 한눈에** | [README](README.md) |

## 🔗 외부 사이트

- **[Landing](../)** — 프로젝트 소개 + 강조 4영역 (히어로 5카드)
- **[발표 슬라이드](../slides/)** — Marp 로 빌드된 발표 자료 (졸업 데모 + 면접 대비)
- **[운영 Swagger UI](http://15.165.95.129:8080/swagger-ui/index.html)** — 실시간 API 진실의 원천
- **[학습 노트 공개판 (goospel.github.io)](https://goospel.github.io/notes/)** — 일반화된 학습 노트 (Spring 비동기 / AWS SSM / CORS — 더 추가 예정)

## 📚 누적 정책

- `claude-docs/*` 는 작업 중 Claude 와 사용자가 함께 만든 모든 영구 기록 — **모든 PR sweep 단계에서 갱신**.
- 새 학습 / 새 함정 / 새 의사결정은 즉시 해당 문서에 추가 후 PR 본문에 sweep 결과 명시.
- 이 사이트는 **dev push 시 자동 빌드 / 배포** — release 전에도 최신 상태 반영.

---

!!! info "검색 단축키"
    ⌘K (Mac) / Ctrl+K (Windows) 로 전체 문서 검색. 코드 블록 / 표 / 헤딩 모두 검색됨.
