package hoseo.moodiary.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 캘린더의 하루 한 칸.
 *
 * <p>응답 배열은 그 달의 모든 날짜를 채워서 반환한다 — 글 없는 날도 {@code postId=null, emoji=null}로 포함.
 *
 * <p><b>emoji 필드</b>는 PR 4(AI 비동기 응답) 머지 전까지는 항상 {@code null}.
 * PR 4 완료 후 후속 PR 에서 {@code Post LEFT JOIN AiResponse} 로 채운다.
 * FE 는 {@code emoji == null}이면 placeholder(회색 점 등) 표시.
 */
@Getter
@Builder
public class CalendarDayResponseDto {

    /** KST 기준 일자. ISO-8601 "YYYY-MM-DD". */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    /** 그 날의 마지막 글(created_at MAX) ID. 글이 없으면 null. */
    private UUID postId;

    /** AI 가 만든 기분 이모지(1자). PR 4 머지 전까지 항상 null. */
    private String emoji;
}
