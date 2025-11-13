package Hwai_team.UniTime.domain.course.dto;
import lombok.Getter;

@Getter
public class TakenCourseToggleRequest {
    private Long userId;         // 필수
    private Boolean taken;       // true=체크, false=해제
}