// src/main/java/Hwai_team/UniTime/domain/timetable/dto/TimetableItemDto.java
package Hwai_team.UniTime.domain.timetable.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableItemDto {

    private Long id;            // 수정 시 사용, 새로 추가되는 과목은 null 가능

    private String courseName;  // "자료구조"
    private String dayOfWeek;   // "MON" | "TUE" | ...
    private Integer startPeriod;
    private Integer endPeriod;
    private String room;        // "IT-101"
    private String category;    // "전공" / "교양" 등
    private Long courseId;
}