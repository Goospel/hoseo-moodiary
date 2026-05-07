package hoseo.moodiary.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PostResponseDto {

    private UUID id;
    private String title;
    private String content;
}
