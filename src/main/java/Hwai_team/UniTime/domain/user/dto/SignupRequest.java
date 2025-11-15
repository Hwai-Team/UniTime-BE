// src/main/java/Hwai_team/UniTime/domain/user/dto/SignupRequest.java
package Hwai_team.UniTime.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String email;            // 이메일
    private String password;         // 비밀번호
    private String name;             // 이름
    private String studentId;        // 학번
    private String department;       // 학과
    private Integer grade;           // 학년
    private Integer graduationYear;  // 졸업년도
}