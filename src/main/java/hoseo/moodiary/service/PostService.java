package hoseo.moodiary.service;

import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.repository.PostJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostJpaRepository repository;

    public void create(PostRequestDto requestDto) {
        Post savedPost = repository.save(requestDto.toEntity());

    }

}
