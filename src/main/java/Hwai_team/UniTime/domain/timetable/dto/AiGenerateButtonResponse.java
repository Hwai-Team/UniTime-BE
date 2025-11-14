package Hwai_team.UniTime.domain.timetable.dto;

// 버튼 노출 + 시간표 생성에 넘길 정리된 메시지까지 포함
public record AiGenerateButtonResponse(
        boolean visible,
        String reason,
        String suggestionText,      // "AI로 이번 학기 시간표를 자동으로..." 같은 문구
        String timetableContextMsg  // ✅ 시간표 생성 쪽에 넘길 정리된 메시지
) {}