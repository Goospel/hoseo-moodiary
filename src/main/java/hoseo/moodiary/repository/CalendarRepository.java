package hoseo.moodiary.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.entitiy.QPost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 캘린더 전용 조회 — QueryDSL 기반.
 *
 * <p>PR 5 에서 QueryDSL 첫 도입. {@link PostJpaRepository}와는 별도로 둔다 —
 * 캘린더는 일자별 그룹핑 등 도메인 고유 쿼리가 누적될 가능성이 있어 책임을 분리.
 *
 * <p><b>인덱스 권장</b>: {@code post(user_id, created_at)} 복합 인덱스.
 * {@code ddl-auto: update}는 인덱스를 보장하지 않으므로 (운영 트래픽 증가 시) 수동 DDL 필요.
 */
@Repository
@RequiredArgsConstructor
public class CalendarRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 사용자의 [startInclusive, endExclusive) 범위 글을 created_at 내림차순으로 조회.
     *
     * <p>일자별 마지막 글 추출은 호출 측(Service)에서 in-memory 그룹핑. 한 달 단위 데이터량(최대 수백 row)
     * 이라 굳이 DB 윈도우 함수까지 가지 않는다.
     */
    public List<Post> findByUserIdAndPeriod(UUID userId,
                                            LocalDateTime startInclusive,
                                            LocalDateTime endExclusive) {
        QPost post = QPost.post;
        return queryFactory
                .selectFrom(post)
                .where(
                        post.user.id.eq(userId),
                        post.createdAt.goe(startInclusive),
                        post.createdAt.lt(endExclusive)
                )
                .orderBy(post.createdAt.desc())
                .fetch();
    }
}
