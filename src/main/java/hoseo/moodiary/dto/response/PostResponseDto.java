package hoseo.moodiary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Schema(description = "게시글 응답")
@Getter
@Builder
public class PostResponseDto {

    @Schema(description = "게시글 ID")
    private UUID id;

    @Schema(description = "게시글 제목", example = "오늘의 기분")
    private String title;

    @Schema(description = "게시글 내용", example = "오늘은 기분이 좋았다.")
    private String content;
}
