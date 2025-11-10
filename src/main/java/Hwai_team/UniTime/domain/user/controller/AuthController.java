// src/main/java/Hwai_team/UniTime/domain/user/controller/AuthController.java
package Hwai_team.UniTime.domain.user.controller;

import Hwai_team.UniTime.domain.user.dto.*;
import Hwai_team.UniTime.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
            summary = "로그아웃",
            description = """
                클라이언트에서 가지고 있던 JWT(access/refresh)를 삭제하면 됩니다.
                서버는 현재 별도 세션을 유지하지 않기 때문에,
                이 API는 '로그아웃 요청이 왔다'는 것을 기록하거나
                추후 블랙리스트 로직을 붙일 때 사용할 수 있습니다.
                """
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal String email) {
        System.out.println("로그아웃 요청한 사용자 이메일 = " + email);
        // TODO: 나중에 refresh 토큰 블랙리스트 처리 같은 거 하고 싶으면 여기서 하면 됨

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "액세스 토큰 재발급",
            description = """
            로그인 시 받은 리프레시 토큰을 이용해 새로운 Access Token을 발급합니다.
            body는 다음 형태로 전송해야 합니다:
            {
              "refreshToken": "<로그인 때 받은 refreshToken 값>"
            }
            """
    )
    public ResponseEntity<TokenResponse> refresh(
            @org.springframework.web.bind.annotation.RequestBody RefreshTokenRequest request
    ) {
        TokenResponse response = userService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}