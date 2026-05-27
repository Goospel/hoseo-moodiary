package hoseo.moodiary.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 활성화. {@code @Async} 어노테이션이 달린 메서드를 백그라운드 스레드에서 실행.
 *
 * <p>PR 4-pre 단계에선 executor 빈을 별도 정의하지 않고 Spring 기본값에 맡긴다 — 졸업 데모 트래픽
 * 기준 충분. 실 운영 부하 측정 후 (PR 4-final 머지 이후) {@code ThreadPoolTaskExecutor} 외부화 검토.
 *
 * <p>사용처: {@code AiResponseService.processAsync(UUID postId)} — PR 4-pre 의 유일한 {@code @Async} 메서드.
 *
 * <p><b>T-019 안전망</b>: 새 빈 추가가 운영 부팅에 영향을 주지 않는지
 * {@link hoseo.moodiary.MoodiaryApplicationTests#contextLoads()} 가 자동 검증.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
