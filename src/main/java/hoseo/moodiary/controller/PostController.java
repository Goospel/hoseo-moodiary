package hoseo.moodiary.controller;

import hoseo.moodiary.dto.request.PostRequestDto;
import hoseo.moodiary.dto.response.AiResponseDto;
import hoseo.moodiary.dto.response.PostResponseDto;
import hoseo.moodiary.service.AiResponseService;
import hoseo.moodiary.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
    private final AiResponseService aiResponseService;

    @Operation(summary = "게시글 생성",
            description = "새 일기를 작성한다. 작성자는 현재 인증된 사용자로 자동 설정. "
                    + "AI 응답은 비동기로 트리거되어 PENDING 상태로 즉시 반환 — 결과는 GET /post/{id}/ai-response 폴링.")
    @ApiResponse(responseCode = "201", description = "생성 성공 — 새 게시글 UUID 반환")
    @ApiResponse(responseCode = "401", description = "미인증")
    @PostMapping("/post")
    public ResponseEntity<UUID> post(@AuthenticationPrincipal UUID userId,
                                     @Valid @RequestBody PostRequestDto requestDto) {
        UUID postId = service.create(userId, requestDto);
        // create() 의 @Transactional 가 메서드 return 시점에 commit. 이 줄에서 호출하면 새 Async
        // 트랜잭션이 PENDING row 를 안전하게 select 가능 (race 없음).
        aiResponseService.triggerAsync(postId);
        return ResponseEntity.status(HttpStatus.CREATED).body(postId);
    }

    @Operation(summary = "내 게시글 전체 조회",
            description = "현재 사용자가 작성한 게시글만 반환. 정렬(기본 일기날짜 최신순) + 선택적 필터 지원.\n\n"
                    + "- `from`/`to`: 일기 날짜(postDate) 범위 (둘 다 inclusive, yyyy-MM-dd). 한쪽만 줘도 됨.\n"
                    + "- `keyword`: 제목/내용 부분일치 (대소문자 무시).\n"
                    + "- `sort`: `필드,방향` 형식. 필드는 `postDate`/`createdAt`, 방향은 `asc`/`desc`. 기본 `postDate,desc`.")
    @ApiResponse(responseCode = "200", description = "조회 성공 (빈 배열 가능)")
    @ApiResponse(responseCode = "400", description = "잘못된 정렬/방향, 날짜 범위 역전, 또는 날짜 형식 오류")
    @ApiResponse(responseCode = "401", description = "미인증")
    @GetMapping("/post")
    public ResponseEntity<List<PostResponseDto>> getAll(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "일기 날짜 하한 (inclusive, yyyy-MM-dd)", example = "2026-05-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "일기 날짜 상한 (inclusive, yyyy-MM-dd)", example = "2026-05-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "제목/내용 키워드 (부분일치, 대소문자 무시)", example = "여행")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "정렬 기준 '필드,방향' (필드: postDate|createdAt, 방향: asc|desc)", example = "postDate,desc")
            @RequestParam(required = false, defaultValue = "postDate,desc") String sort) {
        return ResponseEntity.ok(service.getAllPosts(userId, from, to, keyword, sort));
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

    @Operation(summary = "게시글 삭제", description = "본인 글만 삭제 가능. 연결된 AI 응답도 함께 제거.")
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

    @Operation(summary = "AI 응답 폴링",
            description = "비동기 AI 추론의 현재 상태/결과를 조회. 본인 글만 가능. "
                    + "현재 PR 4-pre 는 Stub 어댑터 — 호출 직후 거의 즉시 DONE 으로 전이.")
    @ApiResponse(responseCode = "200", description = "조회 성공 — status 가 PENDING / DONE / FAILED")
    @ApiResponse(responseCode = "401", description = "미인증")
    @ApiResponse(responseCode = "403", description = "본인 글이 아님")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 게시글 또는 AI 응답")
    @GetMapping("/post/{id}/ai-response")
    public ResponseEntity<AiResponseDto> getAiResponse(@AuthenticationPrincipal UUID userId,
                                                       @PathVariable UUID id) {
        return ResponseEntity.ok(aiResponseService.getByPostId(userId, id));
    }
}
