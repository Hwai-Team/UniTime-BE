// src/main/java/Hwai_team/UniTime/domain/timetable/dto/TimetableUpdateRequest.java
package Hwai_team.UniTime.domain.timetable.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Data
public class TimetableUpdateRequest {
    private String title;
    private List<Item> items;

    @Data
    public static class Item {
        private Long courseId;          // 필수
        private String dayOfWeek;       // 선택: 보내면 코스값 대신 이 값 사용
        private Integer startPeriod;    // 선택
        private Integer endPeriod;      // 선택
        private String room;            // 선택
    }
}