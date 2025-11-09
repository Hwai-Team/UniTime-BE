// src/main/java/Hwai_team/UniTime/domain/user/controller/AuthController.java
package Hwai_team.UniTime.domain.user.controller;

import Hwai_team.UniTime.domain.user.dto.AuthResponse;
import Hwai_team.UniTime.domain.user.dto.LoginRequest;
import Hwai_team.UniTime.domain.user.dto.SignupRequest;
import Hwai_team.UniTime.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "회원가입 및 로그인 / 토큰 API")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "새 사용자를 등록하고 Access/Refresh 토큰을 발급합니다.")
    @ApiResponse(responseCode = "200", description = "회원가입 및 토큰 발급 성공")
    @RequestBody(
            description = "회원가입 요청 바디 예시",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SignupRequest.class),
                    examples = @ExampleObject(
                            name = "테스트 회원가입",
                            summary = "기본 테스트 계정",
                            value = """
                                {
                                  "email": "testuser@unitime.com",
                                  "password": "Test1234!",
                                  "name": "테스트유저",
                                  "department": "컴퓨터공학과",
                                  "grade": 2,
                                  "studentId": "20251234"
                                }
                                """
                    )
            )
    )
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @org.springframework.web.bind.annotation.RequestBody SignupRequest request
    ) {
        return ResponseEntity.ok(userService.signup(request));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 Access/Refresh 토큰을 발급합니다.")
    @ApiResponse(responseCode = "200", description = "로그인 및 토큰 발급 성공")
    @RequestBody(
            description = "로그인 요청 바디 예시",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(
                            name = "테스트 로그인",
                            summary = "기본 테스트 계정",
                            value = """
                                    {
                                      "email": "testuser@unitime.com",
                                      "password": "Test1234!"
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@org.springframework.web.bind.annotation.RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @Operation(
            summary = "토큰 재발급",
            description = """
                    Authorization 헤더에 담긴 Refresh Token으로
                    새로운 Access/Refresh 토큰을 재발급합니다.

                    헤더 예시:
                    Authorization: Bearer {refreshToken}
                    """
    )
    @ApiResponse(responseCode = "200", description = "토큰 재발급 성공")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader(name = "Authorization", required = true, defaultValue = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.REFRESH_TOKEN_EXAMPLE")
            String authorizationHeader
    ) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더 형식이 올바르지 않습니다. 'Bearer {token}' 형태여야 합니다.");
        }

        String refreshToken = authorizationHeader.substring(7);
        return ResponseEntity.ok(userService.refresh(refreshToken));
    }
}