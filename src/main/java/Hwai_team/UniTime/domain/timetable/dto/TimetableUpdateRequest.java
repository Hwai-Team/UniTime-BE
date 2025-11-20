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

        private Long itemId;

        private Long courseId;       // DB 강의면 존재, 이미지강의면 null
        private String courseName;   // 🔥 이미지 인식 시 필수
        private String category;     // 전선/전필/교선/교필 or null

        private String dayOfWeek;    // MON/TUE...
        private Integer startPeriod;
        private Integer endPeriod;
        private String room;
    }
}