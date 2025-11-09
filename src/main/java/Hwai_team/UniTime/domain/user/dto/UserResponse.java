// src/main/java/Hwai_team/UniTime/domain/user/dto/UserResponse.java
package Hwai_team.UniTime.domain.user.dto;

import Hwai_team.UniTime.domain.user.entity.User;
import lombok.Getter;

@Getter
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String studentId;
    private String department;
    private Integer grade;

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.studentId = user.getStudentId();
        this.department = user.getDepartment();
        this.grade = user.getGrade();
    }
}