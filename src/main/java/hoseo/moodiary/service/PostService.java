package hoseo.moodiary.service;

import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.repository.PostJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostJpaRepository repository;

    public PostResponseDto create(PostRequestDto requestDto) {
        Post savedPost = repository.save(requestDto.toEntity());
        return PostResponseDto.builder()
                .id(savedPost.getId())
                .title(savedPost.getTitle())
                .content(savedPost.getContent())
                .build();
    }

    public List<PostResponseDto> getAllPosts() {
        // 페이징은 나중에.
        return repository.findAll().stream()
                .map(post -> PostResponseDto.builder()
                        .id(post.getId())
                        .content(post.getContent())
                        .title(post.getTitle())
                        .build())
                .toList();
    }
}
