package Hwai_team.UniTime.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private Long userId;          // 어떤 유저가 보냈는지
    private String message;       // 유저 질문
    private String conversationId; // (선택) 같은 대화 세션 ID, 없으면 새로 생성
}