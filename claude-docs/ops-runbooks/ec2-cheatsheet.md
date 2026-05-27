# EC2 운영 치트시트

자주 쓰는 명령어 모음. EC2 SSH 접속 후 사용.

## 배포 / 컨테이너

```bash
# 컨테이너 상태
docker ps

# 로그 (실시간)
docker logs -f hoseo-moodiary

# 로그 (최근 100줄)
docker logs --tail=100 hoseo-moodiary

# 수동 재배포 (CD 못 기다릴 때)
docker compose pull && docker compose up -d

# 환경변수 주입 확인 (값 노출 X, 길이만)
docker exec hoseo-moodiary printenv JWT_SECRET | wc -c
```

> **`docker compose` (스페이스) vs `docker-compose` (하이픈)** — EC2 환경 의존 ([T-023](../troubleshooting.md#t-023) 참조).

## DB (RDS)

```bash
# .env 로드
set -a; source ~/.env; set +a

# RDS 접속 (비번 프롬프트)
mysql -h "$RDS_ENDPOINT" -u "$RDS_USERNAME" -p moodiary

# 또는 환경변수로 비번 자동 (보안 경고 없음)
MYSQL_PWD="$RDS_PASSWORD" mysql -h "$RDS_ENDPOINT" -u "$RDS_USERNAME" moodiary

# 테스트 데이터 정리 (졸업 데모 직전)
MYSQL_PWD="$RDS_PASSWORD" mysql -h "$RDS_ENDPOINT" -u "$RDS_USERNAME" moodiary -e "
  DELETE FROM post WHERE user_id IN (SELECT user_id FROM users WHERE user_email LIKE '%@test.com');
  DELETE FROM users WHERE user_email LIKE '%@test.com';
"
```

## GitHub Actions

- 워크플로우: https://github.com/Goospel/hoseo-moodiary/actions
- CI 트리거: `PR → dev`
- CD 트리거: `push → main`
- CD 인프라 fail 시 0번째 진단: [githubstatus.com](https://www.githubstatus.com/) ([T-024](../troubleshooting.md#t-024))

## 트러블슈팅 1차 확인 표

| 증상 | 1차 확인 |
|---|---|
| 8080 응답 없음 | `docker ps` (컨테이너 살아있나) → `docker logs` |
| 500 응답 | `docker logs --tail=200 hoseo-moodiary` → DB 연결 / 스키마 |
| 401인데 이상함 | 토큰 만료 (24h) → 재로그인. JWT_SECRET 변경됐는지도 확인 |
| 403인데 이상함 | 본인 토큰 맞나 + `userId` 일치 확인 |
| RDS 접속 안됨 | RDS Security Group 인바운드 확인 |
| CD 실패 | GitHub Actions 로그 → SSM 단계 / OIDC 자격증명 |
| CD 인프라 단계 fail | **0번째 — [githubstatus.com](https://www.githubstatus.com/)** 확인 ([T-024](../troubleshooting.md#t-024)) |

> 막혔던 사례 누적 → [`troubleshooting.md`](../troubleshooting.md)
