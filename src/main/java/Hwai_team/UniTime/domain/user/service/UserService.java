// src/main/java/Hwai_team/UniTime/domain/user/service/UserService.java
package Hwai_team.UniTime.domain.user.service;

import Hwai_team.UniTime.domain.user.dto.AuthResponse;
import Hwai_team.UniTime.domain.user.dto.LoginRequest;
import Hwai_team.UniTime.domain.user.dto.SignupRequest;
import Hwai_team.UniTime.domain.user.dto.UserResponse;
import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
import Hwai_team.UniTime.global.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입
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
                .build();

        User saved = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(saved.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(saved.getEmail());

        return new AuthResponse(accessToken, refreshToken, new UserResponse(saved));
    }

    // 로그인
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

    // 리프레시 토큰으로 액세스 토큰 재발급
    public AuthResponse refresh(String refreshToken) {
        // 1) 리프레시 토큰 유효성 체크
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 2) 토큰에서 사용자 이메일 꺼내기
        String email = jwtTokenProvider.getUserEmail(refreshToken); // JwtTokenProvider에 맞게 구현되어 있어야 함

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 3) 새 액세스 토큰 / 리프레시 토큰 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return new AuthResponse(newAccessToken, newRefreshToken, new UserResponse(user));
    }
}