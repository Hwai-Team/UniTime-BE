// src/main/java/Hwai_team/UniTime/domain/course/dto/CourseRequest.java
package Hwai_team.UniTime.domain.course.dto;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseRequest {

    private Integer gradeYear;
    private String category;
    private String courseCode;
    private String name;
    private String section;
    private Integer credit;
    private String professor;
    private String dayOfWeek;
    private Integer startPeriod;
    private Integer recommendedGrade;
    private Integer endPeriod;
    private String room;
    private Integer hours;
    private String department;

}