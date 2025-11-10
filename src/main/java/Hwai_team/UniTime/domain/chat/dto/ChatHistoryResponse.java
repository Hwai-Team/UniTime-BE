package Hwai_team.UniTime.domain.chat.dto;

import Hwai_team.UniTime.domain.chat.entity.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatHistoryResponse {

    private Long id;
    private Long userId;
    private String role;        // "USER", "ASSISTANT" 이런 거
    private String content;
    private LocalDateTime createdAt;

    public static ChatHistoryResponse from(ChatMessage message) {
        return ChatHistoryResponse.builder()
                .id(message.getId())
                .userId(message.getUser().getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}