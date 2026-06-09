package hoseo.moodiary.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import hoseo.moodiary.dto.request.PostSortField;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.entitiy.QPost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 게시글 목록 조회 — QueryDSL 동적 쿼리 (정렬 + 선택적 필터).
 *
 * <p>{@link PostJpaRepository}(파생 쿼리), {@link CalendarRepository}(캘린더 전용)와 별도로 둔다 —
 * 목록 조회는 from/to/keyword 의 조합이 선택적이라 파생 쿼리로는 메서드 폭발이 일어난다.
 * QueryDSL {@link BooleanBuilder} 로 null 인 조건만 건너뛰어 한 메서드로 모든 조합을 커버.
 *
 * <p><b>인덱스 권장</b>: {@code post(user_id, post_date)} 복합 인덱스. 기본 정렬이 postDate 라
 * user_id 필터 + postDate 정렬을 한 인덱스로 처리 가능. {@code ddl-auto: update} 는 인덱스를
 * 보장하지 않으므로 (운영 트래픽 증가 시) 수동 DDL 필요.
 */
@Repository
@RequiredArgsConstructor
public class PostSearchRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 사용자의 게시글을 조건에 맞게 조회.
     *
     * @param userId    소유자 (필수 — 항상 본인 글만)
     * @param from      postDate 하한 (inclusive, null 이면 무제한)
     * @param to        postDate 상한 (inclusive, null 이면 무제한)
     * @param keyword   제목/내용 부분일치 (null/blank 이면 무시)
     * @param sortField 정렬 기준 (화이트리스트 enum)
     * @param ascending true=오름차순, false=내림차순
     */
    public List<Post> search(UUID userId,
                             LocalDate from,
                             LocalDate to,
                             String keyword,
                             PostSortField sortField,
                             boolean ascending) {
        QPost post = QPost.post;

        BooleanBuilder where = new BooleanBuilder();
        where.and(post.user.id.eq(userId));
        if (from != null) {
            where.and(post.postDate.goe(from));
        }
        if (to != null) {
            where.and(post.postDate.loe(to));
        }
        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            where.and(post.title.containsIgnoreCase(trimmed)
                    .or(post.content.containsIgnoreCase(trimmed)));
        }

        return queryFactory
                .selectFrom(post)
                .where(where)
                .orderBy(orderSpecifiers(post, sortField, ascending))
                .fetch();
    }

    /**
     * 1차 정렬 + 안정적 tiebreaker. 같은 postDate 가 여러 건일 때 순서가 흔들리지 않도록
     * createdAt desc → id asc 를 보조 키로 추가한다 (페이징 도입 시에도 결정적 순서 보장).
     */
    private OrderSpecifier<?>[] orderSpecifiers(QPost post, PostSortField sortField, boolean ascending) {
        ComparableExpressionBase<?> primaryPath = switch (sortField) {
            case POST_DATE -> post.postDate;
            case CREATED_AT -> post.createdAt;
        };

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        orders.add(ascending ? primaryPath.asc() : primaryPath.desc());
        // 보조 키 — 1차 키가 createdAt 이면 중복이므로 제외.
        if (sortField != PostSortField.CREATED_AT) {
            orders.add(post.createdAt.desc());
        }
        orders.add(post.id.asc());
        return orders.toArray(new OrderSpecifier[0]);
    }
}
