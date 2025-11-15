// src/main/java/Hwai_team/UniTime/domain/timetable/dto/TimetableItemDto.java
package Hwai_team.UniTime.domain.timetable.dto;

import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.timetable.entity.TimetableItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableItemDto {

    private Long id;

    // 🔥 여기 3개를 Course 엔티티에서 채운다
    private Long courseId;
    private Integer credit;
    private String professor;

    private String courseName;
    private String dayOfWeek;
    private Integer startPeriod;
    private Integer endPeriod;
    private String room;
    private String category;

    // 🔥 추천 학년 추가
    private Integer recommendedGrade;

    public static TimetableItemDto from(TimetableItem item) {
        Course c = item.getCourse(); // 🔥 ManyToOne course

        return TimetableItemDto.builder()
                .id(item.getId())
                .courseId(c != null ? c.getId() : null)
                .credit(c != null ? c.getCredit() : null)
                .professor(c != null ? c.getProfessor() : null)
                .courseName(item.getCourseName())
                .dayOfWeek(item.getDayOfWeek())
                .startPeriod(item.getStartPeriod())
                .endPeriod(item.getEndPeriod())
                .room(item.getRoom())
                .category(item.getCategory())
                .recommendedGrade(c != null ? c.getRecommendedGrade() : null) // 🔥 여기서 채움
                .build();
    }
}