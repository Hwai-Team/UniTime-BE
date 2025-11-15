// src/main/java/Hwai_team/UniTime/domain/timetable/dto/TimetableResponse.java
package Hwai_team.UniTime.domain.timetable.dto;

import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public static TimetableResponse from(Timetable timetable) {
        return TimetableResponse.builder()
                .id(timetable.getId())
                .year(timetable.getYear())
                .semester(timetable.getSemester())
                .title(timetable.getTitle())
                .items(
                        timetable.getItems() == null
                                ? List.of()
                                : timetable.getItems().stream()
                                .map(TimetableItemDto::from)   // 🔥 여기 중요
                                .collect(Collectors.toList())
                )
                .build();
    }
}