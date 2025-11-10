package Hwai_team.UniTime.domain.user.controller;

import Hwai_team.UniTime.domain.user.dto.UserProfileResponse;
import Hwai_team.UniTime.domain.user.dto.UserProfileUpdateRequest;
import Hwai_team.UniTime.domain.user.service.UserService;
import Hwai_team.UniTime.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "User", description = "유저/프로필 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "프로필 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다. (Authorization 헤더에 Access Token 필요)",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        UserProfileResponse profile = userService.getProfile(userDetails.getId());
        return ResponseEntity.ok(profile);
    }

    @Operation(
            summary = "프로필 수정",
            description = """
                    현재 로그인한 사용자의 프로필 정보를 수정합니다.
                    Authorization 헤더에 Access Token을 포함해야 합니다.
                    """,
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @RequestBody(
            description = "수정할 프로필 정보 (모든 필드는 선택적으로 수정 가능)",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserProfileUpdateRequest.class),
                    examples = @ExampleObject(
                            name = "프로필 수정 예시",
                            summary = "사용자 프로필 정보 수정 요청 예시",
                            value = """
                                    {
                                      "name": "김서울",
                                      "studentId": "20210001",
                                      "department": "컴퓨터공학과",
                                      "grade": 3,
                                      "graduationYear": 2026
                                    }
                                    """
                    )
            )
    )
    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @org.springframework.web.bind.annotation.RequestBody UserProfileUpdateRequest request
    ) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        Long userId = userDetails.getId();
        UserProfileResponse response = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}