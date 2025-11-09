// src/main/java/Hwai_team/UniTime/domain/user/dto/SignupRequest.java
package Hwai_team.UniTime.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String email;
    private String password;
    private String name;
    private String studentId;
    private String department;
    private Integer grade;
}