package hoseo.moodiary.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.entitiy.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Schema(description = "게시글 생성 요청")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PostRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    @Schema(description = "게시글 제목", example = "오늘의 기분")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Schema(description = "게시글 내용", example = "오늘은 기분이 좋았다.")
    private String content;

    /**
     * 일기 날짜 (선택). 누락 시 서버가 {@code LocalDate.now()} 로 폴백 — 기본 동작은 "오늘 일기".
     * "지나간 날짜의 일기" 작성 시나리오에서 사용자가 명시. {@code ISO_LOCAL_DATE} 포맷 (yyyy-MM-dd).
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "일기 날짜 (yyyy-MM-dd, 누락 시 오늘)", example = "2026-05-29")
    private LocalDate postDate;

    public Post toEntity(User user) {
        return Post.builder()
                .title(title)
                .content(content)
                .postDate(postDate != null ? postDate : LocalDate.now())
                .user(user)
                .build();
    }
}
