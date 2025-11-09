package Hwai_team.UniTime.domain.timetable.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "timetable_items")
public class TimetableItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id")
    @JsonBackReference   // 🔥 여기 추가
    private Timetable timetable;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false)
    private String dayOfWeek;

    @Column(nullable = false)
    private Integer startPeriod;

    @Column(nullable = false)
    private Integer endPeriod;

    @Column
    private String room;

    @Column
    private String category;

    public void setTimetable(Timetable timetable) {
        this.timetable = timetable;
    }
}