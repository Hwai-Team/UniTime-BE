package Hwai_team.UniTime.domain.chat.repository;

import Hwai_team.UniTime.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUser_IdOrderByCreatedAtAsc(Long userId);

    void deleteByUser_Id(Long userId);
}