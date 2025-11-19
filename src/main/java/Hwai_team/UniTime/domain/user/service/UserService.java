// src/main/java/Hwai_team/UniTime/domain/user/service/UserService.java
package Hwai_team.UniTime.domain.user.service;

import Hwai_team.UniTime.domain.user.dto.*;
import Hwai_team.UniTime.domain.user.dto.UserProfileResponse;
import Hwai_team.UniTime.domain.user.dto.UserProfileUpdateRequest;
import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
import Hwai_team.UniTime.global.jwt.JwtTokenProvider;
import Hwai_team.UniTime.domain.user.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * <회원가입 서비스>
     * 회원가입 기능을 합니다
     *
     * @Author 김민호
     * @param SignupRequest
     * @return accessToken, refreshToken, UserResponse
     * @throws IllegalArgumentException 이미 가입된 이메일일 경우 발생
     */
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .studentId(request.getStudentId())
                .department(request.getDepartment())
                .grade(request.getGrade())
                .graduationYear(request.getGraduationYear())
                .build();

        User saved = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(saved.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(saved.getEmail());

        return new AuthResponse(accessToken, refreshToken, new UserResponse(saved));
    }

    /**
     * 로그인을 수행하고 액세스/리프레시 토큰과 사용자 정보를 반환한다.
     *
     * @param request 로그인 요청 정보(이메일, 비밀번호)
     * @return 인증 성공 시 생성된 accessToken, refreshToken, 사용자 정보가 담긴 AuthResponse
     * @throws IllegalArgumentException 이메일 또는 비밀번호가 일치하지 않을 경우 발생
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return new AuthResponse(accessToken, refreshToken, new UserResponse(user));
    }


    /**
     * 리프레시 토큰을 검증하고 새로운 액세스/리프레시 토큰을 재발급한다.
     *
     * @param refreshToken 클라이언트가 보낸 기존 리프레시 토큰
     * @return 새로 발급된 accessToken, refreshToken을 담은 TokenResponse
     * @throws IllegalArgumentException 리프레시 토큰이 없거나, 비어 있거나, 유효하지 않거나, 해당 토큰의 유저가 존재하지 않을 경우 발생
     *
     */
    public TokenResponse refresh(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("리프레시 토큰이 비었습니다.");
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        String email = jwtTokenProvider.getUserEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다."));

        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * <유저 프로필 수정 서비스>
     * 사용자의 프로필을 수정한다.
     *
     * @Author 김민호
     * @param userId, UserProfileUpdateRequest
     * @return UserProfileResponse
     * @throws IllegalArgumentException -> userId가 존재 하지 않을때
     */
    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + userId));

        // null 아닌 값만 업데이트 (부분 수정 가능하게)
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getStudentId() != null) {
            user.setStudentId(request.getStudentId());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
        if (request.getGrade() != null) {
            user.setGrade(request.getGrade());
        }
        if (request.getGraduationYear() != null) {
            user.setGraduationYear(request.getGraduationYear());
        }

        return UserProfileResponse.from(user);
    }

    /**
     * <유저 프로필 조회 서비스>
     *
     * 사용자의 프로필을 조회 합니다.
     *
     * @Author 김민호
     * @param userId
     * @return  UserProfileResponse
     * @throws IllegalArgumentException -> userId가 존재 하지 않을때
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + userId));  // error

        return UserProfileResponse.builder()
                .userId(user.getId())
                .studentId(user.getStudentId())
                .department(user.getDepartment())
                .grade(user.getGrade())
                .name(user.getName())
                .graduationYear(user.getGraduationYear())
                .build();
    }
}