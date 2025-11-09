package Hwai_team.UniTime.domain.course.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 학년 (1, 2, 3, 4 ...)
    @Column(name = "grade_year")
    private Integer gradeYear;

    // 이수구분 (전선, 전필, 교선 등)
    @Column(nullable = false, length = 20)
    private String category;

    // 학수번호 (예: EN1003)
    @Column(name = "course_code", nullable = false, length = 30)
    private String courseCode;

    // 교과목명
    @Column(nullable = false, length = 200)
    private String name;

    // 분반 (01, 02, 10 ...)
    @Column(length = 10)
    private String section;

    // 학점
    @Column
    private Integer credit;

    // 담당교수
    @Column(length = 50)
    private String professor;

    // 요일 (월, 화, 수, Thu, Fri 등. 나중에 필요하면 MON/TUE로 변환해도 됨)
    @Column(length = 10)
    private String dayOfWeek;

    // 시작 교시 (21, 23 같은 숫자)
    @Column
    private Integer startPeriod;

    // 끝 교시
    @Column
    private Integer endPeriod;

    // 강의실
    @Column(length = 50)
    private String room;

    // Course 엔티티 안에 추가
    public void updateFromRequest(Hwai_team.UniTime.domain.course.dto.CourseRequest req) {
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
