package hoseo.moodiary.controller;

import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Post", description = "게시글 API")
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService service;

    @Operation(summary = "게시글 생성", description = "새로운 게시글을 작성합니다.")
    @ApiResponse(responseCode = "201", description = "게시글 생성 성공")
    @PostMapping("/post")
    public ResponseEntity<PostResponseDto> post(@RequestBody PostRequestDto requestDto) {
        PostResponseDto responseDto = service.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseDto);
    }

    @Operation(summary = "게시글 전체 조회", description = "모든 게시글을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공")
    @GetMapping("/post")
    public ResponseEntity<List<PostResponseDto>> getAll() {
        List<PostResponseDto> allPosts = service.getAllPosts();
        return ResponseEntity.status(HttpStatus.OK)
                .body(allPosts);
    }
}
