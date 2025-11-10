package Hwai_team.UniTime.domain.chat.repository;

import Hwai_team.UniTime.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 나중에 문맥 이어서 쓰고 싶을 때 사용할 수 있는 메서드
    List<ChatMessage> findTop20ByUserIdAndConversationIdOrderByCreatedAtAsc(
            Long userId,
            String conversationId
    );
}