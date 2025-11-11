// src/main/java/Hwai_team/UniTime/domain/timetable/dto/TimetableResponse.java
package Hwai_team.UniTime.domain.timetable.dto;

import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.timetable.entity.TimetableItem;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableResponse {

    private Long id;
    private Integer year;
    private Integer semester;
    private String title;

    private List<TimetableItemDto> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 엔티티 → DTO 변환 편의 메서드
    public static TimetableResponse from(Timetable timetable) {
        return TimetableResponse.builder()
                .id(timetable.getId())
                .year(timetable.getYear())
                .semester(timetable.getSemester())
                .title(timetable.getTitle())
                .createdAt(timetable.getCreatedAt())
                .updatedAt(timetable.getUpdatedAt())
                .items(
                        timetable.getItems() == null ? List.of()
                                : timetable.getItems().stream()
                                .map(TimetableResponse::toItemDto)
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static TimetableItemDto toItemDto(TimetableItem item) {
        return TimetableItemDto.builder()
                .id(item.getId())
                .courseName(item.getCourseName())
                .dayOfWeek(item.getDayOfWeek())
                .startPeriod(item.getStartPeriod())
                .endPeriod(item.getEndPeriod())
                .room(item.getRoom())
                .category(item.getCategory())
                .build();
    }
}