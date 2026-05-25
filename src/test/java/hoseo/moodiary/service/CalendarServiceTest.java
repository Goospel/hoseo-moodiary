package hoseo.moodiary.service;

import hoseo.moodiary.dto.response.CalendarDayResponseDto;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.entitiy.User;
import hoseo.moodiary.exception.CalendarInvalidRangeException;
import hoseo.moodiary.repository.CalendarRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * CalendarService 단위 테스트.
 *
 * <p>일자별 그룹핑 / 빈 날 채움 / 유효성 검증 분기를 모두 검증.
 *
 * <p>{@code Post.createdAt}은 BaseEntity 의 protected 필드라 reflection 으로 주입한다 —
 * {@code @CreatedDate} auditing 은 실제 save 시점에만 채워지기 때문.
 */
@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private CalendarRepository calendarRepository;

    @InjectMocks
    private CalendarService calendarService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Nested
    @DisplayName("getMonthlyCalendar — 월별 캘린더 조회")
    class GetMonthlyCalendar {

        @Test
        @DisplayName("그 달에 글이 0개면 모든 날짜에 postId=null, emoji=null")
        void emptyMonth() {
            given(calendarRepository.findByUserIdAndPeriod(eq(USER_ID), any(), any()))
                    .willReturn(List.of());

            List<CalendarDayResponseDto> result =
                    calendarService.getMonthlyCalendar(USER_ID, 2026, 5);

            // 5월은 31일
            assertThat(result).hasSize(31);
            assertThat(result).allMatch(day -> day.getPostId() == null);
            assertThat(result).allMatch(day -> day.getEmoji() == null);
            assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 5, 1));
            assertThat(result.get(30).getDate()).isEqualTo(LocalDate.of(2026, 5, 31));
        }

        @Test
        @DisplayName("윤년 2월은 29개 항목")
        void leapFebruary() {
            given(calendarRepository.findByUserIdAndPeriod(eq(USER_ID), any(), any()))
                    .willReturn(List.of());

            List<CalendarDayResponseDto> result =
                    calendarService.getMonthlyCalendar(USER_ID, 2024, 2);

            assertThat(result).hasSize(29);
            assertThat(result.get(28).getDate()).isEqualTo(LocalDate.of(2024, 2, 29));
        }

        @Test
        @DisplayName("일부 채워진 달 — 글 있는 날만 postId 채워짐, emoji 는 항상 null")
        void partiallyFilled() {
            UUID p1 = UUID.randomUUID();
            UUID p2 = UUID.randomUUID();
            // posts 는 repository 에서 created_at desc 로 정렬됨 — 같은 순서로 mock
            given(calendarRepository.findByUserIdAndPeriod(eq(USER_ID), any(), any()))
                    .willReturn(List.of(
                            postWithCreatedAt(p2, LocalDateTime.of(2026, 5, 10, 9, 0)),
                            postWithCreatedAt(p1, LocalDateTime.of(2026, 5, 3, 14, 30))
                    ));

            List<CalendarDayResponseDto> result =
                    calendarService.getMonthlyCalendar(USER_ID, 2026, 5);

            assertThat(result).hasSize(31);

            // 1일: 글 없음
            assertThat(result.get(0).getPostId()).isNull();
            // 3일: p1
            assertThat(result.get(2).getPostId()).isEqualTo(p1);
            // 10일: p2
            assertThat(result.get(9).getPostId()).isEqualTo(p2);
            // 11일 ~ 31일: 글 없음
            assertThat(result.subList(10, 31)).allMatch(d -> d.getPostId() == null);
            // emoji 는 항상 null (PR 4 머지 전)
            assertThat(result).allMatch(d -> d.getEmoji() == null);
        }

        @Test
        @DisplayName("하루에 글이 여러 개면 created_at 가 가장 늦은 글의 postId 만 노출")
        void multiplePostsSameDay() {
            UUID earlier = UUID.randomUUID();
            UUID later = UUID.randomUUID();
            // repository 는 desc 정렬해서 반환 — later 가 먼저
            given(calendarRepository.findByUserIdAndPeriod(eq(USER_ID), any(), any()))
                    .willReturn(List.of(
                            postWithCreatedAt(later,   LocalDateTime.of(2026, 5, 15, 23, 50)),
                            postWithCreatedAt(earlier, LocalDateTime.of(2026, 5, 15,  8,  0))
                    ));

            List<CalendarDayResponseDto> result =
                    calendarService.getMonthlyCalendar(USER_ID, 2026, 5);

            assertThat(result.get(14).getPostId()).isEqualTo(later);
        }

        @Test
        @DisplayName("응답 배열은 date 오름차순")
        void dateAscending() {
            given(calendarRepository.findByUserIdAndPeriod(eq(USER_ID), any(), any()))
                    .willReturn(List.of());

            List<CalendarDayResponseDto> result =
                    calendarService.getMonthlyCalendar(USER_ID, 2026, 5);

            for (int i = 1; i < result.size(); i++) {
                assertThat(result.get(i).getDate())
                        .isAfter(result.get(i - 1).getDate());
            }
        }
    }

    @Nested
    @DisplayName("validation — year/month 범위")
    class Validation {

        @Test
        @DisplayName("year < 2020 이면 CalendarInvalidRangeException")
        void yearBelowMin() {
            assertThatThrownBy(() -> calendarService.getMonthlyCalendar(USER_ID, 2019, 5))
                    .isInstanceOf(CalendarInvalidRangeException.class)
                    .hasMessageContaining("year는 2020 이상");
        }

        @Test
        @DisplayName("year 가 현재 연도 + 2 이상이면 CalendarInvalidRangeException")
        void yearTooFarFuture() {
            int tooFar = LocalDate.now().getYear() + 2;
            assertThatThrownBy(() -> calendarService.getMonthlyCalendar(USER_ID, tooFar, 5))
                    .isInstanceOf(CalendarInvalidRangeException.class);
        }

        @Test
        @DisplayName("month = 0 이면 CalendarInvalidRangeException")
        void monthZero() {
            assertThatThrownBy(() -> calendarService.getMonthlyCalendar(USER_ID, 2026, 0))
                    .isInstanceOf(CalendarInvalidRangeException.class);
        }

        @Test
        @DisplayName("month = 13 이면 CalendarInvalidRangeException")
        void monthThirteen() {
            assertThatThrownBy(() -> calendarService.getMonthlyCalendar(USER_ID, 2026, 13))
                    .isInstanceOf(CalendarInvalidRangeException.class);
        }
    }

    // --- helpers ---

    private static Post postWithCreatedAt(UUID postId, LocalDateTime createdAt) {
        User user = User.builder().email("a@b.com").password("HASHED").nickname("n").build();
        setField(user, "id", USER_ID);
        Post post = Post.builder().title("t").content("c").user(user).build();
        setField(post, "id", postId);
        setField(post, "createdAt", createdAt);
        return post;
    }

    /** {@link Post#id}는 Post 자체에, {@link Post#getCreatedAt}은 BaseEntity 에 있다 — 슈퍼클래스 탐색. */
    private static void setField(Object target, String fieldName, Object value) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Field not found: " + fieldName);
    }
}
