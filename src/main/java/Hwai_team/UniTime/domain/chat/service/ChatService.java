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

        String userMessage = request.getMessage();

        // 🔥 시간표 의도 감지
        boolean timetableIntent = isTimetableIntent(userMessage);
        TimetablePlanDto plan = null;
        String reply;

        if (timetableIntent) {
            // 🔥 시간표 조건 추출
            plan = extractTimetablePlan(userMessage);

            // 🔥 왼쪽 채팅에는 고정 멘트만 출력 (GPT 호출 막음)
            reply = "요청해 준 조건으로 시간표를 생성해볼게!\n오른쪽 시간표 영역에서 확인해줘.";

        } else {
            // 🔥 졸업요건/일반대화 프롬프트 자동 선택
            String systemPrompt = PromptTemplates.resolveChatSystemPrompt(userMessage);

            reply = openAiClient.askChat(
                    systemPrompt,
                    userMessage
            );
        }

        // 저장
        chatMessageRepository.save(ChatMessage.builder()
                .user(user).role("USER").content(userMessage).build());

        chatMessageRepository.save(ChatMessage.builder()
                .user(user).role("ASSISTANT").content(reply).build());

        return ChatResponse.builder()
                .reply(reply)
                .timetablePlan(timetableIntent)
                .plan(plan)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> getChatHistory(Long userId) {
        return chatMessageRepository.findByUser_IdOrderByCreatedAtAsc(userId)
                .stream()
                .map(ChatHistoryResponse::from)
                .toList();
    }

    @Transactional
    public void deleteChatHistory(Long userId) {
        chatMessageRepository.deleteByUser_Id(userId);
    }


    // ============================================
    // 🔥 더 강력한 시간표 의도 감지 로직
    // ============================================
    private boolean isTimetableIntent(String message) {
        if (message == null) return false;

        String m = message.replaceAll("\\s+", "").toLowerCase();

        // 시간표 관련 키워드
        boolean hasTimetableWord =
                m.contains("시간표") ||
                        m.contains("수강") ||
                        m.contains("강의") ||
                        m.contains("course") ||
                        m.contains("timetable");

        // 행동 키워드 (형태 다양하게 대응)
        boolean hasActionWord =
                m.contains("짜") ||
                        m.contains("만들") ||
                        m.contains("추천") ||
                        m.contains("골라") ||
                        m.contains("조합") ||
                        m.contains("편성");

        return hasTimetableWord && hasActionWord;
    }


    // ============================================
    // 시간표 조건 추출
    // ============================================
    private TimetablePlanDto extractTimetablePlan(String message) {
        if (message == null) return null;

        String m = message.replaceAll("\\s+", "");

        Integer targetCredits = null;
        String preferredDays = null;
        String timePreference = null;
        String avoidDays = null;

        Matcher creditMatcher = Pattern.compile("(\\d{1,2})학점").matcher(m);
        if (creditMatcher.find()) {
            try { targetCredits = Integer.parseInt(creditMatcher.group(1)); } catch (Exception ignored) {}
        }

        Matcher dayMatcher = Pattern.compile("([월화수목금토일]{2,4})").matcher(m);
        if (dayMatcher.find()) {
            preferredDays = dayMatcher.group(1);
        }

        if (m.contains("오전")) timePreference = "오전";
        else if (m.contains("오후")) timePreference = "오후";
        else if (m.contains("저녁") || m.contains("야간")) timePreference = "저녁";

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