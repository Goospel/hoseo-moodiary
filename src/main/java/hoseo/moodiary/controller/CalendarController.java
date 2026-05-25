package hoseo.moodiary.controller;

import hoseo.moodiary.dto.response.CalendarDayResponseDto;
import hoseo.moodiary.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Calendar API.
 *
 * <p>월별 캘린더 한 화면을 채울 데이터. 그 달의 모든 날짜를 빈 날 포함 배열로 반환한다 —
 * FE 는 인덱스로 바로 격자에 매핑할 수 있다.
 *
 * <p>인증 필수 — {@code @AuthenticationPrincipal UUID userId}로 본인 데이터만 조회.
 *
 * <p>{@code emoji}는 현재 항상 {@code null} (PR 4 머지 후 채워짐).
 */
@Tag(name = "Calendar", description = "월별 캘린더 API")
@RestController
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService service;

    @Operation(summary = "월별 캘린더 조회",
            description = "지정한 year/month 의 모든 일자를 반환. 글 없는 날은 postId/emoji 가 null. "
                    + "하루에 여러 글이 있으면 마지막 글의 postId 만 노출.")
    @ApiResponse(responseCode = "200", description = "조회 성공 — 그 달 일수만큼의 배열")
    @ApiResponse(responseCode = "400", description = "year/month 범위 위반")
    @ApiResponse(responseCode = "401", description = "미인증")
    @GetMapping("/calendar")
    public ResponseEntity<List<CalendarDayResponseDto>> getMonthly(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "조회 연도 (2020 ~ 현재+1)", example = "2026")
            @RequestParam int year,
            @Parameter(description = "조회 월 (1-12)", example = "5")
            @RequestParam int month) {
        return ResponseEntity.ok(service.getMonthlyCalendar(userId, year, month));
    }
}
