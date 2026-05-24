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
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Post", description = "게시글 API")
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService service;

    @Operation(summary = "게시글 생성", description = "새로운 게시글을 작성합니다.")
    @ApiResponse(responseCode = "201", description = "게시글 생성 성공")
    @PostMapping("/post")
    public ResponseEntity<UUID> post(@Valid @RequestBody PostRequestDto requestDto) {
        UUID postId = service.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postId);
    }

    @Operation(summary = "게시글 전체 조회", description = "모든 게시글을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공")
    @GetMapping("/post")
    public ResponseEntity<List<PostResponseDto>> getAll() {
        List<PostResponseDto> allPosts = service.getAllPosts();
        return ResponseEntity.status(HttpStatus.OK)
                .body(allPosts);
    }

    @Operation(summary = "게시글 단건 조회", description = "ID로 게시글 하나를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 조회 성공")
    @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    @GetMapping("/post/{id}")
    public ResponseEntity<PostResponseDto> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getPost(id));
    }

    @Operation(summary = "게시글 수정", description = "ID로 게시글을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 수정 성공")
    @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    @PutMapping("/post/{id}")
    public ResponseEntity<PostResponseDto> update(@PathVariable UUID id,
                                                  @Valid @RequestBody PostRequestDto requestDto) {
        return ResponseEntity.ok(service.update(id, requestDto));
    }

    @Operation(summary = "게시글 삭제", description = "ID로 게시글을 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "게시글 삭제 성공")
    @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    @DeleteMapping("/post/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
