package Hwai_team.UniTime.domain.timetable.dto;

import Hwai_team.UniTime.domain.timetable.entity.AiTimetable;
import lombok.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
@Setter
@NoArgsConstructor   // ✅ 추가
@AllArgsConstructor
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

    public static AiTimetableResponse from(AiTimetable entity) {
        return AiTimetableResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .timetableId(
                        entity.getTimetable() != null
                                ? entity.getTimetable().getId()
                                : null
                )
                .prompt(entity.getPrompt())
                .resultSummary(entity.getResultSummary())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}