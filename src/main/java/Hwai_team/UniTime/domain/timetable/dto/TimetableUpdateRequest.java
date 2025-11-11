// src/main/java/Hwai_team/UniTime/domain/timetable/dto/TimetableUpdateRequest.java
package Hwai_team.UniTime.domain.timetable.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class TimetableUpdateRequest {

    private String title;                       // 제목 변경 가능
    private List<TimetableItemDto> items;      // 현재 시간표의 전체 아이템 목록
}