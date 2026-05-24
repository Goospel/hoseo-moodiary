package hoseo.moodiary.exception;

import java.util.UUID;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(UUID id) {
        super("게시글을 찾을 수 없습니다. id=" + id);
    }
}
