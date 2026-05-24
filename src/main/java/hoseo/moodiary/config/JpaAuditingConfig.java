package hoseo.moodiary.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 전용 설정.
 *
 * <p>{@link EnableJpaAuditing}은 {@code MoodiaryApplication}에 직접 달지 않고 이 클래스로 분리한다.
 * 그래야 {@code @WebMvcTest} 같은 슬라이스 테스트에서 JPA 인프라(jpaMappingContext 등)를 끌고 들어가
 * "JPA metamodel must not be empty" 오류로 컨텍스트 로딩이 깨지는 것을 피할 수 있다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
