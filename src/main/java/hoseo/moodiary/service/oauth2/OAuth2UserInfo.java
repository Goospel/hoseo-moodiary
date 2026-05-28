package hoseo.moodiary.service.oauth2;

/**
 * Provider 측에서 검증을 거쳐 추출한 사용자 정보.
 *
 * <p>{@link OAuth2Provider#verifyAndExtract(String)} 의 반환 타입. provider 별 응답 (Google {@code tokeninfo}
 * / Kakao {@code /v2/user/me}) 의 차이를 흡수해서 BE 내부의 통일된 형태로 표현.
 *
 * @param providerId provider 측 user 식별자 — Google {@code sub} / Kakao {@code id}
 * @param email      provider 측 이메일 — Google {@code email} / Kakao {@code kakao_account.email}.
 *                   주의: Kakao 는 사용자가 이메일 제공 동의 안 한 경우 빈 값일 수 있다 — PR 12-final 단계에서 정책 결정.
 * @param nickname   provider 측 닉네임 후보 — Google {@code name} / Kakao {@code kakao_account.profile.nickname}.
 *                   우리 도메인의 닉네임 (20자 제한) 보다 길거나 충돌하면 trim / suffix 처리는 OAuth2Service 가 담당.
 */
public record OAuth2UserInfo(
        String providerId,
        String email,
        String nickname
) {
}
