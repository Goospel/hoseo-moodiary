package hoseo.moodiary;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Full ApplicationContext 부팅 안전망.
 *
 * <p><b>왜 필요한가</b> — `@WebMvcTest` 같은 슬라이스 테스트는 컨트롤러 + Security 만 띄우고
 * springdoc / QueryDSL / JPA 인프라 등 자동 구성 빈을 건드리지 않는다. 그래서 운영 jar 의
 * 실제 부팅에서 터지는 bean creation 실패가 단위 테스트로는 절대 감지되지 않는다.
 *
 * <p>이 테스트는 운영과 동일한 클래스패스 + 전체 자동 구성을 띄워 모든 빈이 인스턴스화 되는지 확인한다.
 * 운영 RDS 의존성을 끊기 위해 H2 in-memory 로 datasource 만 오버라이드.
 *
 * <p><b>역사</b> — 원래 `@Disabled` 였다. 그 결과 PR #38 까지 누적된 코드가
 * springdoc-openapi 2.x ↔ Spring Boot 4 의 `TypeInformation` 클래스 이동 비호환을
 * 단 한 번도 빌드 단계에서 못 잡았고, 운영 첫 deploy 에서 컨테이너 restart loop 로 폭발.
 * 다시 disable 하지 말 것.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:moodiary-context-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // H2 가 MySQL 의 'users' 예약어 처리를 흉내내도록 — User 엔티티의 @Table(name="users") 와 호환
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class MoodiaryApplicationTests {

    /**
     * 컨텍스트가 완전히 로드되면 통과. 빈 생성 실패는 즉시 BeanCreationException 으로 드러난다.
     */
    @Test
    void contextLoads() {
    }
}
