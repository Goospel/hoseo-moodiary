package hoseo.moodiary.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.entitiy.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Schema(description = "게시글 생성 요청")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PostRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 255, message = "제목은 255자를 넘을 수 없습니다.")
    @Schema(description = "게시글 제목 (최대 255자)", example = "오늘의 기분")
    private String title;

    /**
     * 본문 길이 상한 — 컬럼은 {@code TEXT} 라 더 길어도 DB 는 받지만, 한도를 명시해 binding 단계에서
     * 400 으로 거르면 거대한 페이로드/실수 입력을 방어 가능. 한도 초과가 DB 까지 가서 500 으로 떨어지는 것
     * (T-036) 보다 깔끔하다.
     */
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 10000, message = "내용은 10000자를 넘을 수 없습니다.")
    @Schema(description = "게시글 내용 (최대 10000자)", example = "오늘은 기분이 좋았다.")
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
