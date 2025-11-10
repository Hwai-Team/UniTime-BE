package Hwai_team.UniTime.domain.chat.service;

import Hwai_team.UniTime.domain.chat.dto.ChatHistoryResponse;
import Hwai_team.UniTime.domain.chat.dto.ChatRequest;
import Hwai_team.UniTime.domain.chat.dto.ChatResponse;
import Hwai_team.UniTime.domain.chat.entity.ChatMessage;
import Hwai_team.UniTime.domain.chat.repository.ChatMessageRepository;
import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
import Hwai_team.UniTime.global.ai.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final OpenAiClient openAiClient;
    private final UserRepository userRepository;


    @Transactional
    public ChatResponse chat(ChatRequest request) {


        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("message는 비어 있을 수 없습니다.");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + request.getUserId()));

        // conversationId 없으면 새로 생성
        String conversationId =
                (request.getConversationId() == null || request.getConversationId().isBlank())
                        ? UUID.randomUUID().toString()
                        : request.getConversationId();

        // 시스템 프롬프트 (나중에 네 서비스 컨셉에 맞게 바꿔도 됨)
        String systemPrompt = """
                너는 UniTime 서비스 안에서 동작하는 대학생 전용 챗봇이야.
                시간표, 수강 과목, 공부 계획, 그리고 UniTime 기능 사용법에 대해
                친절하고 간결하게 한국어로 답변해줘.
                """;

        // GPT에게 질문
        String reply = openAiClient.askChat(systemPrompt, request.getMessage());

        // DB에 유저 메시지 저장
        ChatMessage userMsg = ChatMessage.builder()
                .user(user)
                .role("USER")
                .content(request.getMessage())
                .conversationId(conversationId)
                .build();
        chatMessageRepository.save(userMsg);

        // DB에 GPT 답변 저장
        ChatMessage botMsg = ChatMessage.builder()
                .user(user)
                .role("ASSISTANT")
                .content(reply)
                .conversationId(conversationId)
                .build();
        chatMessageRepository.save(botMsg);

        // 응답 반환
        return ChatResponse.builder()
                .reply(reply)
                .conversationId(conversationId)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> getChatHistory(Long userId) {
        List<ChatMessage> messages =
                chatMessageRepository.findByUser_IdOrderByCreatedAtAsc(userId);

        return messages.stream()
                .map(ChatHistoryResponse::from)
                .toList();
    }

    @Transactional
    public void deleteChatHistory(Long userId) {
        chatMessageRepository.deleteByUser_Id(userId);
    }
}