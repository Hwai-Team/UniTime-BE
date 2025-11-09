// src/main/java/Hwai_team/UniTime/domain/user/dto/AuthResponse.java
package Hwai_team.UniTime.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;
}