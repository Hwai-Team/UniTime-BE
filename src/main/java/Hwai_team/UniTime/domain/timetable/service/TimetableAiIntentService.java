package Hwai_team.UniTime.domain.timetable.service;

import Hwai_team.UniTime.domain.timetable.dto.AiGenerateButtonRequest;
import Hwai_team.UniTime.domain.timetable.dto.AiGenerateButtonResponse;
import org.springframework.stereotype.Service;

@Service
public class TimetableAiIntentService {

    public AiGenerateButtonResponse checkButtonVisibility(AiGenerateButtonRequest request) {
        String text = request.lastUserMessage().toLowerCase();

        // 1차: 키워드 기반 간단 룰
        boolean hit = text.contains("시간표") &&
                (text.contains("만들") || text.contains("추천") || text.contains("짜줘"));

        if (!hit) {
            return new AiGenerateButtonResponse(false, "키워드 매칭 실패", null);
        }

        // 2차: 필요하면 OpenAI한테 의도분류 시켜도 됨
        String suggestion = "AI로 이번 학기 시간표를 자동으로 생성해볼까요?";
        return new AiGenerateButtonResponse(true, "키워드 매칭 성공", suggestion);
    }
}