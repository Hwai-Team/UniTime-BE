package Hwai_team.UniTime.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequest {

    @Schema(description = "이름", example = "김서울")
    private String name;

    @Schema(description = "학번", example = "20210001")
    private String studentId;

    @Schema(description = "학과", example = "컴퓨터공학과")
    private String department;

    @Schema(description = "학년(숫자)", example = "3")
    private Integer grade;

    @Schema(description = "졸업 예정 연도", example = "2026")
    private Integer graduationYear;
}