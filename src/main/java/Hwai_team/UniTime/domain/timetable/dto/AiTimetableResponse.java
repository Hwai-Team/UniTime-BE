// src/main/java/Hwai_team/UniTime/domain/timetable/dto/AiTimetableResponse.java
package Hwai_team.UniTime.domain.timetable.dto;

import Hwai_team.UniTime.domain.timetable.entity.AiTimetable;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTimetableResponse {

    private Long id;
    private Long userId;
    private Long timetableId;

    private String prompt;
    private String title;
    private String userName;
    private String message;
    private String resultSummary;
    private LocalDateTime createdAt;

    private List<TimetableItemDto> items;

    public static AiTimetableResponse from(AiTimetable entity) {
        Timetable t = entity.getTimetable();

        return AiTimetableResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .timetableId(t != null ? t.getId() : null)
                .prompt(entity.getPrompt())
                .title(t != null ? t.getTitle() : null)
                .userName(entity.getUser().getName())
                .message(null)
                .resultSummary(entity.getResultSummary())
                .createdAt(entity.getCreatedAt())
                .items(
                        t != null && t.getItems() != null
                                ? t.getItems().stream()
                                .map(TimetableItemDto::from)
                                .toList()
                                : List.of()
                )
                .build();
    }
}