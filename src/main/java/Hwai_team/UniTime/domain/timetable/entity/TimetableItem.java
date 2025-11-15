// src/main/java/Hwai_team/UniTime/domain/timetable/entity/TimetableItem.java
package Hwai_team.UniTime.domain.timetable.entity;

import Hwai_team.UniTime.domain.course.entity.Course;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "timetable_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TimetableItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔥 FK: course_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // FK: timetable_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id")
    private Timetable timetable;

    private String courseName;
    private String dayOfWeek;
    private Integer startPeriod;
    private Integer endPeriod;
    private String room;
    private String category;

    // 연관관계 편의 메서드
    public void setTimetable(Timetable timetable) {
        this.timetable = timetable;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}