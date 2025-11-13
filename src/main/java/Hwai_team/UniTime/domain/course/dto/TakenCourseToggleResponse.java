package Hwai_team.UniTime.domain.course.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class TakenCourseToggleResponse {
    private Long courseId;
    private Long userId;
    private boolean taken;  // 현재 상태
}