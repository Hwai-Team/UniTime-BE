package Hwai_team.UniTime.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatResponse {

    private String reply;         // GPT 답변
    private String conversationId; // 이 대화가 속한 세션 ID
    private Boolean timetablePlan;
    private TimetablePlanDto plan;
}