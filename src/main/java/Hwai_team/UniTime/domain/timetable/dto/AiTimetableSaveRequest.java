package Hwai_team.UniTime.domain.timetable.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiTimetableSaveRequest {

    private Long userId;        // 어떤 유저의 시간표인지
    private Long timetableId;   // 이미 저장된 Timetable의 id
}