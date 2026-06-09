package hoseo.moodiary.entitiy;

import hoseo.moodiary.config.JpaAuditingConfig;
import hoseo.moodiary.repository.PostJpaRepository;
import hoseo.moodiary.repository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@code post_content} 컬럼 길이 회귀 테스트.
 *
 * <p>버그 제보(FE): 여러 줄(=긴) 일기 본문을 POST /post 하면 500 ({@code 서버 오류가 발생했습니다.})
 * 발생. 원인은 {@code @Column} length 미지정 → JPA 기본 {@code VARCHAR(255)} 매핑이라
 * 255자 초과 본문이 {@code Data too long for column} 으로 터지는 것. 줄바꿈(\n) 자체가 아니라
 * "길이 초과" 가 진짜 원인 — 짧은 글이 201 로 통과하는 것과 일관.
 *
 * <p>이 테스트는 fix 전엔 {@code save} 가 예외로 실패(=재현), fix 후엔 통과(=회귀 방어).
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class PostContentLengthTest {

    @Autowired
    private PostJpaRepository postRepository;

    @Autowired
    private UserJpaRepository userRepository;

    /** 줄바꿈 포함 ~600자 — 평범한 여러 줄 일기 본문 길이. */
    private static final String LONG_MULTILINE_CONTENT = ("""
            오늘은 아침 일찍 일어나서 산책을 했다.
            공원의 공기가 맑아서 기분이 좋았다.
            점심에는 친구를 만나 오랜만에 수다를 떨었다.
            저녁에는 집에서 책을 읽으며 하루를 마무리했다.
            """).repeat(5);

    @Test
    @DisplayName("255자를 넘는 여러 줄 일기 본문도 저장된다 (VARCHAR(255) 회귀 방어)")
    void saveLongMultilineContent_succeeds() {
        User user = userRepository.save(
                User.createLocal("diarist@moodiary.test", "$2a$10$hashplaceholderhashplaceholderhash", "다이어리스트"));
        assertThat(LONG_MULTILINE_CONTENT.length()).isGreaterThan(255); // 전제 확인

        Post post = Post.builder()
                .title("오늘의 기분")
                .content(LONG_MULTILINE_CONTENT)
                .postDate(LocalDate.of(2026, 6, 9))
                .user(user)
                .build();

        assertThatCode(() -> postRepository.saveAndFlush(post)).doesNotThrowAnyException();
        assertThat(postRepository.findById(post.getId()))
                .get()
                .extracting(Post::getContent)
                .isEqualTo(LONG_MULTILINE_CONTENT);
    }
}
