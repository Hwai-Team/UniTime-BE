// src/main/java/Hwai_team/UniTime/domain/course/dto/CourseSearchCond.java
package Hwai_team.UniTime.domain.course.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseSearchCond {

    private Integer gradeYear;   // 1,2,3,4...
    private String category;     // 전선, 전필, 교선...
    private String keyword;      // 과목명 검색용
}