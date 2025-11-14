package Hwai_team.UniTime.domain.timetable.dto;

import java.util.List;

public record AiGenerateButtonRequest(
        Long userId,
        String lastUserMessage,   // 방금 유저가 챗봇에 친 문장
        List<String> recentUserMessages // (선택) 직전 몇 개까지 같이 보낼 수도 있음
) {}