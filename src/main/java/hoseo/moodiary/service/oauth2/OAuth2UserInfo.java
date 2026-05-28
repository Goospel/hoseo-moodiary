package hoseo.moodiary.service.oauth2;

/**
 * Provider 측에서 검증을 거쳐 추출한 사용자 정보.
 *
 * <p>{@link OAuth2Provider#verifyAndExtract(String)} 의 반환 타입. provider 별 응답
 * (현재 Google {@code tokeninfo}) 의 차이를 흡수해서 BE 내부의 통일된 형태로 표현 —
 * 다른 provider 부활 시 같은 record 로 재사용.
 *
 * @param providerId provider 측 user 식별자 — Google {@code sub} 클레임.
 * @param email      provider 측 이메일 — Google {@code email}. {@code email_verified=true} 검증은 Provider 구현이 책임.
 * @param nickname   provider 측 닉네임 후보 — Google {@code name}.
 *                   우리 도메인의 닉네임 (20자 제한) 보다 길거나 충돌하면 trim / suffix 처리는 OAuth2Service 가 담당.
 */
public record OAuth2UserInfo(
        String providerId,
        String email,
        String nickname
) {
}
