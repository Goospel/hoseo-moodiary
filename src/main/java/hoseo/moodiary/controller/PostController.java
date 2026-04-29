package hoseo.moodiary.controller;

import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
