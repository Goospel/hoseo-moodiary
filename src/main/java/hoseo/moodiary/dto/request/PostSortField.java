package hoseo.moodiary.dto.request;

import hoseo.moodiary.exception.InvalidPostSearchException;

import java.util.Arrays;

/**
 * 게시글 목록 정렬 화이트리스트.
 *
 * <p>사용자가 보낸 {@code sort} 쿼리 파라미터의 필드명을 그대로 {@code Sort.by()} / QueryDSL 에 넘기면
 * 임의 컬럼 정렬(인덱스 미스, 내부 필드 노출)을 허용하게 된다 — 그래서 허용 필드를 enum 으로 못박는다.
 *
 * <p>API 노출명({@link #apiName})만 사용자 입력과 매칭한다. enum 상수명이 아니라 camelCase 노출명을
 * 받는 이유: FE 계약이 {@code sort=postDate,desc} 형태라 DB/엔티티 표현과 분리.
 */
public enum PostSortField {

    POST_DATE("postDate"),
    CREATED_AT("createdAt");

    private final String apiName;

    PostSortField(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }

    /**
     * 사용자 입력 필드명 → enum. 화이트리스트에 없으면 400 ({@link InvalidPostSearchException}).
     */
    public static PostSortField from(String apiName) {
        return Arrays.stream(values())
                .filter(f -> f.apiName.equalsIgnoreCase(apiName))
                .findFirst()
                .orElseThrow(() -> new InvalidPostSearchException(
                        "지원하지 않는 정렬 기준입니다: " + apiName + " (가능: postDate, createdAt)"));
    }
}
