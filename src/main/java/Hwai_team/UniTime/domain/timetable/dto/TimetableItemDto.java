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

    // 🔥 Course 엔티티에서 채울 수 있는 정보들
    private Long courseId;
    private Integer credit;
    private String professor;

    private String courseName;
    private String dayOfWeek;
    private Integer startPeriod;
    private Integer endPeriod;
    private String room;
    private String category;

    // 🔥 추천 학년
    private Integer recommendedGrade;

    public static TimetableItemDto from(TimetableItem item) {
        Course c = item.getCourse(); // null 가능

        Long courseId      = (c != null) ? c.getId() : null;
        Integer credit     = (c != null) ? c.getCredit() : null;
        String professor   = (c != null) ? nullToEmpty(c.getProfessor()) : null;

        // 우선순위: item에 있는 값 > course 값 > ""
        String courseName  = firstNonEmpty(
                item.getCourseName(),
                (c != null ? c.getName() : null)
        );

        String dayOfWeek   = nullToEmpty(item.getDayOfWeek());
        Integer start      = item.getStartPeriod();
        Integer end        = item.getEndPeriod();
        String room        = nullToEmpty(item.getRoom());

        String category    = firstNonEmpty(
                item.getCategory(),
                (c != null ? c.getCategory() : null)
        );

        Integer recommendedGrade = (c != null) ? c.getRecommendedGrade() : null;

        return TimetableItemDto.builder()
                .id(item.getId())
                .courseId(courseId)
                .credit(credit)
                .professor(professor)
                .courseName(courseName)
                .dayOfWeek(dayOfWeek)
                .startPeriod(start)
                .endPeriod(end)
                .room(room)
                .category(category)
                .recommendedGrade(recommendedGrade)
                .build();
    }

    // ===== 내부 유틸 =====
    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return "";
    }
}