package Hwai_team.UniTime.domain.chat.service;

import Hwai_team.UniTime.domain.chat.dto.ChatHistoryResponse;
import Hwai_team.UniTime.domain.chat.dto.ChatRequest;
import Hwai_team.UniTime.domain.chat.dto.ChatResponse;
import Hwai_team.UniTime.domain.chat.dto.TimetablePlanDto;
import Hwai_team.UniTime.domain.chat.entity.ChatMessage;
import Hwai_team.UniTime.domain.chat.repository.ChatMessageRepository;
import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
import Hwai_team.UniTime.global.ai.OpenAiClient;
import Hwai_team.UniTime.global.ai.PromptTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import Hwai_team.UniTime.global.ai.PromptTemplates;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // 🔹 굳이 conversationId 쓸 거면 이렇게 "유저 기반 가짜 id"만 만들어서
        //    응답에만 내려주고, DB에는 저장 안 함.
        String conversationId = "USER-" + user.getId();

        // 시스템 프롬프트
        String reply = openAiClient.askChat(
                PromptTemplates.CHAT_SYSTEM_PROMPT,
                request.getMessage()
        );

        // === 여기서 "시간표 만들 의도" 감지 + 플랜 추출 ===
        boolean timetableIntent = isTimetableIntent(request.getMessage());
        TimetablePlanDto plan = null;
        if (timetableIntent) {
            plan = extractTimetablePlan(request.getMessage());
        }

        // DB에 유저 메시지 저장 (❌ conversationId 제거)
        ChatMessage userMsg = ChatMessage.builder()
                .user(user)
                .role("USER")
                .content(request.getMessage())
                .build();
        chatMessageRepository.save(userMsg);

        // DB에 GPT 답변 저장 (❌ conversationId 제거)
        ChatMessage botMsg = ChatMessage.builder()
                .user(user)
                .role("ASSISTANT")
                .content(reply)
                .build();
        chatMessageRepository.save(botMsg);

        // 응답 반환 (프론트는 conversationId 안 써도 됨, 써도 그냥 이 가짜 값)
        return ChatResponse.builder()
                .reply(reply)
                .timetablePlan(timetableIntent)
                .plan(plan)
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

    // ----------------- 여기부터 유틸 메서드들 -----------------

    private boolean isTimetableIntent(String message) {
        if (message == null) return false;

        String m = message.replaceAll("\\s+", "").toLowerCase();

        boolean hasTimetableWord =
                m.contains("시간표") ||
                        m.contains("수강신청") ||
                        m.contains("시간표짜") ||
                        m.contains("시간표만들");

        boolean hasActionWord =
                m.contains("짜줘") ||
                        m.contains("만들어줘") ||
                        m.contains("추천해줘") ||
                        m.contains("골라줘") ||
                        m.contains("짜줄래") ||
                        m.contains("만들어줄래");

        return hasTimetableWord && hasActionWord;
    }

    private TimetablePlanDto extractTimetablePlan(String message) {
        if (message == null) {
            return null;
        }
        String m = message.replaceAll("\\s+", "");

        Integer targetCredits = null;
        String preferredDays = null;
        String timePreference = null;
        String avoidDays = null;

        Pattern creditPattern = Pattern.compile("(\\d{1,2})학점");
        Matcher creditMatcher = creditPattern.matcher(m);
        if (creditMatcher.find()) {
            try {
                targetCredits = Integer.parseInt(creditMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        Pattern dayPattern = Pattern.compile("([월화수목금토일]{2,4})");
        Matcher dayMatcher = dayPattern.matcher(m);
        if (dayMatcher.find()) {
            preferredDays = dayMatcher.group(1);
        }

        if (m.contains("오전")) {
            timePreference = "오전";
        } else if (m.contains("오후")) {
            timePreference = "오후";
        } else if (m.contains("저녁") || m.contains("야간")) {
            timePreference = "저녁";
        }

        if (m.contains("금요일") || m.contains("금은비우") || m.contains("금피하")) {
            avoidDays = "금요일";
        }

        return TimetablePlanDto.builder()
                .targetCredits(targetCredits)
                .preferredDays(preferredDays)
                .timePreference(timePreference)
                .avoidDays(avoidDays)
                .build();
    }
}