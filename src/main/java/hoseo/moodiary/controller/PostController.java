package hoseo.moodiary.controller;

import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Post API.
 *
 * <p>모든 엔드포인트는 인증 필수. {@code @AuthenticationPrincipal UUID userId}로 현재 사용자 ID를 받는다 —
 * {@code JwtAuthenticationFilter}(PR 2-c)가 토큰 검증 후 principal에 UUID를 박아둔다.
 *
 * <p>소유권: 본인 글만 보고/수정/삭제 가능 (403 Forbidden 매핑).
 */
@Tag(name = "Post", description = "게시글 API")
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService service;

    @Operation(summary = "게시글 생성", description = "새 일기를 작성한다. 작성자는 현재 인증된 사용자로 자동 설정.")
    @ApiResponse(responseCode = "201", description = "생성 성공 — 새 게시글 UUID 반환")
    @ApiResponse(responseCode = "401", description = "미인증")
    @PostMapping("/post")
    public ResponseEntity<UUID> post(@AuthenticationPrincipal UUID userId,
                                     @Valid @RequestBody PostRequestDto requestDto) {
        UUID postId = service.create(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(postId);
    }

    @Operation(summary = "내 게시글 전체 조회", description = "현재 사용자가 작성한 게시글만 반환.")
    @ApiResponse(responseCode = "200", description = "조회 성공 (빈 배열 가능)")
    @ApiResponse(responseCode = "401", description = "미인증")
    @GetMapping("/post")
    public ResponseEntity<List<PostResponseDto>> getAll(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(service.getAllPosts(userId));
    }

    @Operation(summary = "게시글 단건 조회", description = "본인 글만 조회 가능.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "미인증")
    @ApiResponse(responseCode = "403", description = "본인 글이 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 게시글")
    @GetMapping("/post/{id}")
    public ResponseEntity<PostResponseDto> getOne(@AuthenticationPrincipal UUID userId,
                                                  @PathVariable UUID id) {
        return ResponseEntity.ok(service.getPost(userId, id));
    }

    @Operation(summary = "게시글 수정", description = "본인 글만 수정 가능.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "401", description = "미인증")
    @ApiResponse(responseCode = "403", description = "본인 글이 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 게시글")
    @PutMapping("/post/{id}")
    public ResponseEntity<PostResponseDto> update(@AuthenticationPrincipal UUID userId,
                                                  @PathVariable UUID id,
                                                  @Valid @RequestBody PostRequestDto requestDto) {
        return ResponseEntity.ok(service.update(userId, id, requestDto));
    }

    @Operation(summary = "게시글 삭제", description = "본인 글만 삭제 가능.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "401", description = "미인증")
    @ApiResponse(responseCode = "403", description = "본인 글이 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 게시글")
    @DeleteMapping("/post/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId,
                                       @PathVariable UUID id) {
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
