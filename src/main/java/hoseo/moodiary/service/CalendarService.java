package hoseo.moodiary.service;

import hoseo.moodiary.dto.response.CalendarDayResponseDto;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.exception.CalendarInvalidRangeException;
import hoseo.moodiary.repository.CalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 월별 캘린더 도메인 서비스.
 *
 * <p>응답 규약:
 * <ul>
 *   <li>그 달의 실제 일수(28/29/30/31)만큼 모든 날짜를 반환 — 빈 날도 {@code postId=null, emoji=null}.</li>
 *   <li>하루에 여러 글이면 마지막 글(created_at MAX)의 postId 만 노출.</li>
 *   <li>{@code emoji}는 PR 4 머지 전까지 항상 {@code null}. 후속 PR 에서 AiResponse LEFT JOIN.</li>
 * </ul>
 *
 * <p>시간대 처리:
 * <ul>
 *   <li>일자 그룹핑 기준 = {@link #KST}(Asia/Seoul). 한국 사용자 대상 서비스라 KST 일자가 자연스럽다.</li>
 *   <li>{@code Post.createdAt}은 {@link LocalDateTime}이라 자체로는 zone 정보가 없다 —
 *       DB(RDS)와 컨테이너의 시간대 설정에 의존. 현재 시스템에서 저장된 값을 KST 시계로 해석.</li>
 *   <li>운영 환경 TZ 검증/명시화는 별도 PR 에서 다룰 예정. 일관성이 깨질 위험이 보이면 그 시점에
 *       {@code hibernate.jdbc.time_zone} 설정을 추가하고 회귀 테스트를 붙인다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final CalendarRepository calendarRepository;

    /** 일자 그룹핑 기준 시간대. */
    static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 허용 최소 연도. 그 이전은 의미상 잘못된 요청. */
    private static final int MIN_YEAR = 2020;

    /** year 유효 범위 위반 시 메시지 (한 곳으로 통일). */
    private static final String INVALID_RANGE_MESSAGE = "year는 2020 이상, month는 1-12 사이여야 합니다.";

    public List<CalendarDayResponseDto> getMonthlyCalendar(UUID userId, int year, int month) {
        validateRange(year, month);

        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime startInclusive = ym.atDay(1).atStartOfDay();
        LocalDateTime endExclusive = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<Post> posts = calendarRepository.findByUserIdAndPeriod(userId, startInclusive, endExclusive);

        // KST 기준 일자별 마지막 글 (created_at MAX) 추출.
        // posts 는 created_at desc 정렬 — 처음 만난 날짜가 그 날의 마지막 글이다.
        Map<LocalDate, Post> lastPostByDay = new HashMap<>();
        for (Post p : posts) {
            LocalDate day = p.getCreatedAt().toLocalDate();
            lastPostByDay.putIfAbsent(day, p);
        }

        // 그 달의 모든 일자 채우기.
        List<CalendarDayResponseDto> result = new ArrayList<>(ym.lengthOfMonth());
        for (int dayOfMonth = 1; dayOfMonth <= ym.lengthOfMonth(); dayOfMonth++) {
            LocalDate date = ym.atDay(dayOfMonth);
            Post last = lastPostByDay.get(date);
            result.add(CalendarDayResponseDto.builder()
                    .date(date)
                    .postId(last != null ? last.getId() : null)
                    .emoji(null) // PR 4 머지 후 AiResponse LEFT JOIN 으로 채운다
                    .build());
        }
        return result;
    }

    private void validateRange(int year, int month) {
        int currentYear = LocalDate.now(KST).getYear();
        if (year < MIN_YEAR || year > currentYear + 1 || month < 1 || month > 12) {
            throw new CalendarInvalidRangeException(INVALID_RANGE_MESSAGE);
        }
    }
}
