package hoseo.moodiary.controller;

import hoseo.moodiary.config.SecurityConfig;
import hoseo.moodiary.dto.response.CalendarDayResponseDto;
import hoseo.moodiary.exception.CalendarInvalidRangeException;
import hoseo.moodiary.security.JwtTokenProvider;
import hoseo.moodiary.service.CalendarService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CalendarController 웹 레이어 단위 테스트.
 */
@WebMvcTest(CalendarController.class)
@Import(SecurityConfig.class)
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalendarService calendarService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static RequestPostProcessor asUser(UUID userId) {
        return authentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Nested
    @DisplayName("GET /calendar — 월별 캘린더 조회")
    class GetMonthly {

        @Test
        @DisplayName("정상 요청이면 200 과 그 달 일수만큼의 배열")
        void success() throws Exception {
            UUID p = UUID.randomUUID();
            given(calendarService.getMonthlyCalendar(eq(USER_ID), eq(2026), eq(5)))
                    .willReturn(List.of(
                            CalendarDayResponseDto.builder()
                                    .date(LocalDate.of(2026, 5, 1))
                                    .postId(null).emoji(null).build(),
                            CalendarDayResponseDto.builder()
                                    .date(LocalDate.of(2026, 5, 2))
                                    .postId(p).emoji(null).build()
                    ));

            mockMvc.perform(get("/calendar")
                            .with(asUser(USER_ID))
                            .param("year", "2026")
                            .param("month", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].date").value("2026-05-01"))
                    .andExpect(jsonPath("$[0].postId").isEmpty())
                    .andExpect(jsonPath("$[0].emoji").isEmpty())
                    .andExpect(jsonPath("$[1].postId").value(p.toString()));
        }

        @Test
        @DisplayName("year 또는 month 범위 위반이면 400 + 메시지")
        void invalidRange() throws Exception {
            willThrow(new CalendarInvalidRangeException(
                    "year는 2020 이상, month는 1-12 사이여야 합니다."))
                    .given(calendarService).getMonthlyCalendar(eq(USER_ID), anyInt(), anyInt());

            mockMvc.perform(get("/calendar")
                            .with(asUser(USER_ID))
                            .param("year", "1999")
                            .param("month", "5"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("year는 2020 이상, month는 1-12 사이여야 합니다."));
        }

        @Test
        @DisplayName("year 파라미터 누락이면 400 (Spring 기본 처리)")
        void missingYear() throws Exception {
            mockMvc.perform(get("/calendar")
                            .with(asUser(USER_ID))
                            .param("month", "5"))
                    .andExpect(status().isBadRequest());

            verify(calendarService, never()).getMonthlyCalendar(eq(USER_ID), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("미인증 접근 — 401")
    class Unauthenticated {

        @Test
        @DisplayName("인증 없으면 401")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/calendar")
                            .param("year", "2026")
                            .param("month", "5"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("인증이 필요합니다."));

            verify(calendarService, never()).getMonthlyCalendar(eq(USER_ID), anyInt(), anyInt());
        }
    }
}
