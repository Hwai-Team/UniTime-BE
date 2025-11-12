// src/main/java/Hwai_team/UniTime/domain/course/dto/CourseResponse.java
package Hwai_team.UniTime.domain.course.dto;

import Hwai_team.UniTime.domain.course.entity.Course;
import lombok.Getter;

@Getter
public class CourseResponse {

    private Long id;
    private String courseCode;
    private String name;
    private Integer gradeYear;
    private String category;
    private Integer credit;
    private String dayOfWeek;
    private Integer startPeriod;
    private Integer endPeriod;
    private String professor;
    private String room;
    private String section;

    public static CourseResponse from(Course course) {
        CourseResponse res = new CourseResponse();
        res.id = course.getId();
        res.courseCode = course.getCourseCode();
        res.name = course.getName();
        res.gradeYear = course.getGradeYear();
        res.category = course.getCategory();
        res.credit = course.getCredit();
        res.dayOfWeek = course.getDayOfWeek();
        res.startPeriod = course.getStartPeriod();
        res.endPeriod = course.getEndPeriod();
        res.professor = course.getProfessor();
        res.room = course.getRoom();
        res.section = course.getSection();
        return res;
    }
}