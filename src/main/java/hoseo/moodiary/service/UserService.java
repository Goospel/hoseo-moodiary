package hoseo.moodiary.service;

import hoseo.moodiary.dto.request.UserSignupRequestDto;
import hoseo.moodiary.entitiy.User;
import hoseo.moodiary.exception.DuplicateEmailException;
import hoseo.moodiary.exception.DuplicateNicknameException;
import hoseo.moodiary.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserJpaRepository repository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입.
     *
     * <p>중복 체크는 서비스 레이어에서 사전에 거른다. DB의 unique 제약은 동시 가입(race)에 대비한 이중 안전망.
     * 비밀번호는 BCrypt 해시로만 저장하며 평문은 어디에도 남기지 않는다.
     *
     * @return 생성된 회원의 ID
     */
    public UUID signup(UserSignupRequestDto requestDto) {
        if (repository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateEmailException(requestDto.getEmail());
        }
        if (repository.existsByNickname(requestDto.getNickname())) {
            throw new DuplicateNicknameException(requestDto.getNickname());
        }

        User user = User.builder()
                .email(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .nickname(requestDto.getNickname())
                .build();

        return repository.save(user).getId();
    }
}
