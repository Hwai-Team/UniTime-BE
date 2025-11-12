package Hwai_team.UniTime.domain.timetable.dto;

import lombok.Data;

@Data
public class TimetableItemUpdateRequest {
    private Long courseId;  // 과목 선택 시 넘겨줄 ID
    private String dayOfWeek;   // "MON|TUE|WED|THU|FRI"
    private Integer startPeriod;
    private Integer endPeriod;
    private String room;
}