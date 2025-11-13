// src/main/java/Hwai_team/UniTime/domain/timetable/dto/TimetableImageImportResponse.java
package Hwai_team.UniTime.domain.timetable.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableImageImportResponse {

    private List<Item> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String courseName;    // 강의명
        private String courseCode;    // 학수번호
        private String dayOfWeek;     // 요일
        private Integer startPeriod;  // 시작 시간
        private Integer endPeriod;    // 종료 시간
        private String room;          // 강의실
    }
}