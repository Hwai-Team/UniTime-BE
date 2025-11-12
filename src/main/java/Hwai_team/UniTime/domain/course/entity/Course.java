// src/main/java/Hwai_team/UniTime/domain/course/entity/Course.java
package Hwai_team.UniTime.domain.course.entity;

import Hwai_team.UniTime.domain.course.dto.CourseRequest;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 권장
@AllArgsConstructor
@Builder
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 권장 학년 (1,2,3,4…)
    @Column(name = "grade_year")
    private Integer gradeYear;

    @Column(name = "recommended_grade")
    private Integer recommendedGrade;

    // 이수구분 (전선/전필/교선 등)
    @Column(nullable = false, length = 20)
    private String category;

    // 학수번호 (예: EN1003)
    @Column(name = "course_code", nullable = false, length = 30)
    private String courseCode;

    // 교과목명
    @Column(nullable = false, length = 200)
    private String name;

    @Column
    private Integer hours;

    // 분반 (01,02…)
    @Column(length = 10)
    private String section;

    // 학점
    @Column
    private Integer credit;

    // 담당교수
    @Column(length = 50)
    private String professor;

    // 요일 (예: MON/TUE/WED 또는 월/화/수)
    @Column(name = "day_of_week", length = 10)
    private String dayOfWeek;

    // 시작 교시
    @Column(name = "start_period")
    private Integer startPeriod;

    // 종료 교시
    @Column(name = "end_period")
    private Integer endPeriod;

    // 강의실
    @Column(length = 50)
    private String room;

    @Column(length = 100)
    private String department;

    // 편의 메서드: 요청으로부터 값 업데이트
    public void updateFromRequest(CourseRequest req) {
        this.gradeYear = req.getGradeYear();
        this.category = req.getCategory();
        this.courseCode = req.getCourseCode();
        this.name = req.getName();
        this.section = req.getSection();
        this.credit = req.getCredit();
        this.professor = req.getProfessor();
        this.dayOfWeek = req.getDayOfWeek();
        this.startPeriod = req.getStartPeriod();
        this.endPeriod = req.getEndPeriod();
        this.room = req.getRoom();
    }
}