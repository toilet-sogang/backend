package hwalibo.toilet.service.user;

import hwalibo.toilet.domain.user.User;
import hwalibo.toilet.dto.user.request.UserNameUpdateRequest;
import hwalibo.toilet.dto.user.response.UpdatedUser;
import hwalibo.toilet.dto.user.response.UserResponse;
import hwalibo.toilet.dto.user.response.UserUpdateResponse;
import hwalibo.toilet.exception.user.DuplicateUserNameException;
import hwalibo.toilet.exception.user.UserNotFoundException;
import hwalibo.toilet.respository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    // 로그인된 유저 정보 조회
    @Transactional(readOnly = true)
    public UserResponse getUserInfo(User loginUser) {
        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(UserNotFoundException::new);

        // 전체 유저 수
        long totalUsers = userRepository.count();

        // 나보다 리뷰 수가 많은 유저 수
        long higherRank = userRepository.countByNumReviewGreaterThan(
                user.getNumReview() != null ? user.getNumReview() : 0
        );

        // 상위 퍼센트 (정수)
        int rate = totalUsers > 0
                ? (int) Math.ceil(higherRank * 100.0 / totalUsers)
                : 100;

        return UserResponse.from(user, rate);
    }

    @Transactional
    public UserUpdateResponse updateUserName(User loginUser, UserNameUpdateRequest request) {
        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(UserNotFoundException::new);

        String newName = request.getName();

        // ✅ 닉네임 중복 체크
        if (userRepository.existsByName(newName)) {
            throw new DuplicateUserNameException("이미 존재하는 닉네임입니다.");
        }

        user.updateName(newName);

        User updated = userRepository.findById(user.getId())
                .orElseThrow(UserNotFoundException::new);

        return new UserUpdateResponse(UpdatedUser.from(updated));
    }
}