package Hwai_team.UniTime.domain.timetable.dto;

public record AiGenerateButtonResponse(
        boolean showButton,   // true 면 버튼 보여주기
        String reason,        // 왜 그렇게 판단했는지 (디버깅/로그용)
        String suggestionText // 버튼 옆에 띄울 안내 문구 (예: "AI로 시간표 자동 생성해볼까요?")
) {}