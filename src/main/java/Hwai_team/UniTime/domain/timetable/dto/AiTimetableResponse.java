// src/main/java/Hwai_team/UniTime/domain/timetable/dto/AiTimetableResponse.java
package Hwai_team.UniTime.domain.timetable.dto;

import Hwai_team.UniTime.domain.timetable.entity.AiTimetable;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter   // 🔥 이거 중요
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTimetableResponse {

    private Long id;
    private Long userId;

    // 🔥 이 필드가 반드시 있어야 함
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
                .title(entity.getTimetable() != null
                        ? entity.getTimetable().getTitle()
                        : null)
                .userName(entity.getUser().getName())
                .message(null) // 필요하면 엔티티에서 가져오도록 수정
                .resultSummary(entity.getResultSummary())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}