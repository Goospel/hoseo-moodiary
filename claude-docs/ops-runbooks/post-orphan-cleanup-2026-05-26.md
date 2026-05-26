# Runbook · post 테이블 orphan row 클린업 (2026-05-26)

## 배경

PR #24 (Post → User ownership) 머지 후 한참이 지나서야 운영 RDS 에 처음 도달했다 ([T-019](../troubleshooting.md#t-019) 참조). 그 사이 운영 RDS 의 `post` 테이블에는 User 엔티티가 없던 시절에 만들어진 row 가 남아 있다. Hibernate `ddl-auto: update` 가 `user_id` FK 를 추가하려 할 때 이 orphan row 가 제약 조건을 충족하지 못해 ALTER TABLE 이 실패한다:

```
SQLIntegrityConstraintViolationException: Cannot add or update a child row:
a foreign key constraint fails (`moodiary`.`#sql-3e8_3af8`,
CONSTRAINT `FK7ky67sgi7k0ayf22652f7763r`
FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`))
```

이 runbook 은 운영 RDS 에서 한 번만 실행하는 절차다.

## 사전 조건

- EC2 또는 권한 있는 클라이언트에서 RDS 접속 가능
- `RDS_HOST`, `RDS_USERNAME`, `RDS_PASSWORD` 환경변수 (또는 `.env`) 설정
- `mysql` CLI 또는 동등 도구

## 절차

### 1. 백업 (필수)

운영 RDS 의 자동 스냅샷에 의존하지 말고 명시적으로 한 번 뜬다.

```bash
# AWS 콘솔 → RDS → 인스턴스 선택 → 작업 → 스냅샷 생성
# 식별자 예시: moodiary-pre-orphan-cleanup-2026-05-26
```

### 2. orphan row 식별

```sql
USE moodiary;

-- (a) 진단: 전체 post / orphan 개수 출력
SELECT
    (SELECT COUNT(*) FROM post) AS total_posts,
    (SELECT COUNT(*) FROM post WHERE user_id IS NULL) AS posts_with_null_user,
    (SELECT COUNT(*) FROM post p
        WHERE p.user_id IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM users u WHERE u.user_id = p.user_id)
    ) AS posts_with_invalid_user;

-- (b) orphan row 의 ID 와 작성 시점 확인 (지울 대상 미리 봄)
SELECT post_id, post_title, user_id, created_at
FROM post
WHERE user_id IS NULL
   OR user_id NOT IN (SELECT user_id FROM users);
```

> 결과를 PR 본문 혹은 이 runbook 끝의 "실행 기록"에 붙여 보관.

### 3. orphan row 삭제

```sql
START TRANSACTION;

DELETE FROM post
WHERE user_id IS NULL
   OR user_id NOT IN (SELECT user_id FROM users);

-- 영향 행 수 확인 후 commit 결정
SELECT ROW_COUNT() AS deleted_rows;
-- 삭제 행 수가 예상치와 일치하면:
COMMIT;
-- 아니면:
-- ROLLBACK;
```

### 4. 컨테이너 재기동 + 부팅 로그 확인

```bash
# EC2 에서
cd /home/ec2-user  # compose.yaml 위치 — 환경에 따라 조정
docker-compose pull
docker-compose up -d

# Spring 부팅 완료까지 ~30초. 컨테이너 health 확인
docker ps --filter "name=hoseo-moodiary"
docker logs hoseo-moodiary --tail 50 | grep -E "Started|ERROR"
```

부팅 성공 시 로그에 `Started MoodiaryApplication in X seconds` 가 보여야 한다. `BeanCreationException` 이나 `SQLIntegrityConstraintViolationException` 이 또 보이면 PR body 의 "운영 머지 전 필수" 체크리스트 다시 확인.

### 5. 운영 검증

```bash
# Swagger UI
curl -i http://localhost:8080/swagger-ui/index.html | head -3
# 기대: HTTP/1.1 200

# CORS preflight (로컬 origin)
curl -i -X OPTIONS http://localhost:8080/post \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Authorization,Content-Type"
# 기대: HTTP/1.1 200 + Access-Control-Allow-Origin: http://localhost:5173

# CORS preflight (미허용 origin)
curl -i -X OPTIONS http://localhost:8080/post \
  -H "Origin: http://evil.com" \
  -H "Access-Control-Request-Method: GET"
# 기대: HTTP/1.1 403
```

## 실행 기록

### 2026-05-26 (예정)

- [ ] RDS 스냅샷: `<식별자 기록>`
- [ ] 진단 결과: total_posts=___ null_user=___ invalid_user=___
- [ ] 삭제 행 수: ___
- [ ] 부팅 성공: __:__:__
- [ ] CORS preflight 200/403 확인 완료

## 향후 재발 방지

이런 종류의 "엔티티 도입 ↔ 운영 RDS 마이그레이션" 갭은 `ddl-auto: update` 한계의 직접적 결과다. Flyway 도입 (PR 6 예정) 시점에:

1. orphan 데이터 가능성이 있는 새 NOT NULL FK 도입 시 → 별도 데이터 마이그레이션 step 분리
2. 운영 DDL 변경은 PR body 의 "운영 머지 전 필수" 체크리스트에서 명시
3. `ddl-auto: validate` 로 전환 — 미적용 DDL 이 있으면 부팅 시 fail-fast
