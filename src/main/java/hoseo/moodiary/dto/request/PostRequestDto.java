package hoseo.moodiary.dto.request;

import hoseo.moodiary.entitiy.Post;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostRequestDto {

    private String title;

    private String content;

    public Post toEntity() {
        return Post.builder()
                .title(title)
                .content(content)
                .build();
    }
}
