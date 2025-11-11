// src/main/java/Hwai_team/UniTime/domain/timetable/dto/TimetableCreateRequest.java
package Hwai_team.UniTime.domain.timetable.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TimetableCreateRequest {
    private Integer year;      // 2024
    private Integer semester;  // 1, 2
    private String title;      // "24학년도 1학기" (null이면 서비스에서 기본값 생성)
}