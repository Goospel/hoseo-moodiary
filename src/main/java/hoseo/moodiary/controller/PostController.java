package hoseo.moodiary.controller;

import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.service.PostService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService service;

    @PostMapping("/post")
    public ResponseEntity<PostResponseDto> post(@RequestBody PostRequestDto requestDto) {
        PostResponseDto responseDto = service.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseDto);
    }

    @GetMapping("/post")
    public ResponseEntity<List<PostResponseDto>> getAll() {
        List<PostResponseDto> allPosts = service.getAllPosts();
        return ResponseEntity.status(HttpStatus.OK)
                .body(allPosts);
    }
}
