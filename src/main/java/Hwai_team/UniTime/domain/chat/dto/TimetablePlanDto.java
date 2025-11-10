package Hwai_team.UniTime.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetablePlanDto {

    // 목표 학점 (예: 19)
    private Integer targetCredits;

    // 선호 요일 (예: "월수", "월수목")
    private String preferredDays;

    // 시간대 선호 (예: "오전", "오후", "저녁")
    private String timePreference;

    // 피하고 싶은 요일 (예: "금요일")
    private String avoidDays;
}