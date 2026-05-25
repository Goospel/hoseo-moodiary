package hoseo.moodiary.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 진입점 — {@link JPAQueryFactory} Bean.
 *
 * <p>PR 5(Calendar API)에서 처음 도입. 복합 조건 + 동적 쿼리가 필요한 곳에서 주입받아 사용한다.
 *
 * <p>구현 노트
 * <ul>
 *   <li>QueryDSL 자체는 {@code compileOnly} 의존성이라 런타임에는 없다 — Bean 생성 클래스만 컴파일 시
 *       바인딩되고, 실제 호출은 Spring 이 채워 넣은 EntityManager 가 일을 한다.</li>
 *   <li>Q-classes 는 {@code src/main/generated/} 아래에 annotationProcessor 가 만든다
 *       ({@code build.gradle}의 querydsl-apt 참조).</li>
 * </ul>
 */
@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
