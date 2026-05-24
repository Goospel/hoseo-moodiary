package hoseo.moodiary.service;

import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.entitiy.Post;
import hoseo.moodiary.exception.PostNotFoundException;
import hoseo.moodiary.repository.PostJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostJpaRepository repository;

    public UUID create(PostRequestDto requestDto) {
        Post savedPost = repository.save(requestDto.toEntity());
        return savedPost.getId();
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getAllPosts() {
        // todo: 페이지네이션 구현해야됨.
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPost(UUID id) {
        Post post = repository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
        return toResponse(post);
    }

    public PostResponseDto update(UUID id, PostRequestDto requestDto) {
        Post post = repository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
        post.update(requestDto.getTitle(), requestDto.getContent());
        return toResponse(post);
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new PostNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private PostResponseDto toResponse(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .build();
    }
}
