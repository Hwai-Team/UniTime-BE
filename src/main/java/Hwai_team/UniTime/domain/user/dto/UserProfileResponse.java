package Hwai_team.UniTime.domain.user.dto;

import Hwai_team.UniTime.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

    private Long userId;
    private String name;          // 이름
    private String studentId;     // 학번
    private String department;    // 학과
    private Integer grade;        // 학년 (3 이런 숫자)
    private Integer graduationYear; // 졸업년도

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .studentId(user.getStudentId())
                .department(user.getDepartment())
                .grade(user.getGrade())
                .graduationYear(user.getGraduationYear())
                .build();
    }
}