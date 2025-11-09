// src/main/java/Hwai_team/UniTime/domain/timetable/dto/AiTimetableRequest.java
package Hwai_team.UniTime.domain.timetable.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiTimetableRequest {
    private Long userId;    // ✅ AI가 시간표를 만들어 줄 사용자 ID
    private String message; // "월수 위주로 18학점" 같은 자유 텍스트
    private Integer year;   // 학년도
    private Integer semester; // 학기 (1, 2)
}