package Hwai_team.UniTime.domain.timetable.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 시간표 생성 조건(요약 메세지) 반환용 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableSummaryResponse {

    private Long userId;

    /**
     * 유저의 시간표 생성 조건 요약 (AiTimetable.prompt)
     */
    private String summary;
}