// src/main/java/Hwai_team/UniTime/domain/course/dto/CourseResponse.java
package Hwai_team.UniTime.domain.course.dto;

import Hwai_team.UniTime.domain.course.entity.Course;
import lombok.Getter;

@Getter
public class CourseResponse {

    private Long id;
    private Integer gradeYear;
    private String category;
    private String courseCode;
    private String name;
    private String section;
    private Integer credit;
    private String professor;
    private String dayOfWeek;
    private Integer startPeriod;
    private Integer endPeriod;
    private String room;

    public CourseResponse(Course course) {
        this.id = course.getId();
        this.gradeYear = course.getGradeYear();
        this.category = course.getCategory();
        this.courseCode = course.getCourseCode();
        this.name = course.getName();
        this.section = course.getSection();
        this.credit = course.getCredit();
        this.professor = course.getProfessor();
        this.dayOfWeek = course.getDayOfWeek();
        this.startPeriod = course.getStartPeriod();
        this.endPeriod = course.getEndPeriod();
        this.room = course.getRoom();
    }
}