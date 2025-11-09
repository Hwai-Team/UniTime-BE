// src/main/java/Hwai_team/UniTime/domain/timetable/dto/AiTimetableResponse.java
package Hwai_team.UniTime.domain.timetable.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTimetableResponse {

    private Long timetableId;  // 생성된 시간표 ID
    private String title;      // 시간표 제목 (예: "2025-1학기 전공 시간표")
    private String userName;   // 사용자 이름
    private String message;    // 상태 메시지 ("AI 시간표가 성공적으로 생성되었습니다.")
}