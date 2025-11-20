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

    /**
     * 사용자의 입력 메시지를 받아 AI 응답을 생성하고,
     * 시간표 생성 의도 여부를 판별한 뒤 DB에 기록하고 최종 응답을 반환합니다.
     *
     * @param request 유저 ID와 메시지를 포함한 요청 객체
     * @return 생성된 AI 응답, 시간표 플랜 여부, 추출된 시간표 플랜 정보
    */
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

        String userMessage = request.getMessage();

        // 🔥 시간표 의도 감지
        boolean timetableIntent = isTimetableIntent(userMessage);
        TimetablePlanDto plan = null;
        String reply;

        if (timetableIntent) {
            // 🔥 시간표 조건(학점/요일 등) 추출
            plan = extractTimetablePlan(userMessage);

            // 🔥 왼쪽 채팅은 이제 과목 상세를 말하면 안 됨 → 고정 멘트
            reply = "요청해 준 조건으로 시간표를 생성해볼게!\n오른쪽 시간표 영역에서 확인해줘.";

        } else {
            // 🎓 졸업요건 포함 일반 대화 → 질문 분석해서 system prompt 선택
            String systemPrompt = PromptTemplates.resolveChatSystemPrompt(userMessage);

            reply = openAiClient.askChat(
                    systemPrompt,   // ← 핵심 수정!
                    userMessage
            );
        }

        // 메시지 저장
        ChatMessage userMsg = ChatMessage.builder()
                .user(user)
                .role("USER")
                .content(userMessage)
                .build();
        chatMessageRepository.save(userMsg);

        ChatMessage botMsg = ChatMessage.builder()
                .user(user)
                .role("ASSISTANT")
                .content(reply)
                .build();
        chatMessageRepository.save(botMsg);

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
