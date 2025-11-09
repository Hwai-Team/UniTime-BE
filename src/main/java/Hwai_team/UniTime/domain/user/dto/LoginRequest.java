// src/main/java/Hwai_team/UniTime/domain/user/dto/LoginRequest.java
package Hwai_team.UniTime.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    private String email;
    private String password;
}