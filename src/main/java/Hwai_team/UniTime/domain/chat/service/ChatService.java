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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
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

        // conversationId 없으면 새로 생성
        String conversationId =
                (request.getConversationId() == null || request.getConversationId().isBlank())
                        ? UUID.randomUUID().toString()
                        : request.getConversationId();

        // 시스템 프롬프트
        String systemPrompt = """
                너는 UniTime 서비스 안에서 동작하는 대학생 전용 챗봇이야.
                시간표, 수강 과목, 공부 계획, 그리고 UniTime 기능 사용법에 대해
                친절하고 간결하게 한국어로 답변해줘.
                """;

        // GPT에게 답변 요청
        String reply = openAiClient.askChat(systemPrompt, request.getMessage());

        // === 여기서 "시간표 만들 의도" 감지 + 플랜 추출 ===
        boolean timetableIntent = isTimetableIntent(request.getMessage());
        TimetablePlanDto plan = null;
        if (timetableIntent) {
            plan = extractTimetablePlan(request.getMessage());
        }

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

        // 응답 반환 (이제 프론트에서 timetablePlan + plan 보고 버튼 노출 여부 결정)
        return ChatResponse.builder()
                .reply(reply)
                .conversationId(conversationId)
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

    /**
     * 아주 단순한 키워드 기반 "시간표 생성 의도" 감지.
     * 나중에 필요하면 더 똑똑하게 고도화하면 됨.
     */
    private boolean isTimetableIntent(String message) {
        if (message == null) return false;

        String m = message.replaceAll("\\s+", "").toLowerCase();

        // 기본 키워드들
        boolean hasTimetableWord =
                m.contains("시간표") ||
                        m.contains("수강신청") ||
                        m.contains("시간표짜") ||
                        m.contains("시간표만들");

        // “짜줘, 만들어줘, 추천해줘” 같은 동사
        boolean hasActionWord =
                m.contains("짜줘") ||
                        m.contains("만들어줘") ||
                        m.contains("추천해줘") ||
                        m.contains("골라줘") ||
                        m.contains("짜줄래") ||
                        m.contains("만들어줄래");

        return hasTimetableWord && hasActionWord;
    }

    /**
     * 한국어 문장 속에서 대충 정보 뽑는 헐거운 파서.
     * - "19학점" → targetCredits
     * - "월수", "월수목", "화목" 같은 패턴 → preferredDays
     * - "오전/오후/저녁" → timePreference
     * - "금요일은 비우고", "금은 피하고" → avoidDays
     *
     * 완벽할 필요는 없고, 프론트에서 한 번 더 확인 받는 용도라 대충만 박아도 됨.
     */
    private TimetablePlanDto extractTimetablePlan(String message) {
        if (message == null) {
            return null;
        }
        String m = message.replaceAll("\\s+", "");

        Integer targetCredits = null;
        String preferredDays = null;
        String timePreference = null;
        String avoidDays = null;

        // 1) 학점 수 찾기: "19학점", "18학점"
        Pattern creditPattern = Pattern.compile("(\\d{1,2})학점");
        Matcher creditMatcher = creditPattern.matcher(m);
        if (creditMatcher.find()) {
            try {
                targetCredits = Integer.parseInt(creditMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        // 2) 선호 요일: "월수", "월수목", "화목" 등
        //    월/화/수/목/금/토/일 중 2~4글자 연속으로 나오면 그냥 선호 요일이라고 치자
        Pattern dayPattern = Pattern.compile("([월화수목금토일]{2,4})");
        Matcher dayMatcher = dayPattern.matcher(m);
        if (dayMatcher.find()) {
            preferredDays = dayMatcher.group(1);
        }

        // 3) 시간대 선호
        if (m.contains("오전")) {
            timePreference = "오전";
        } else if (m.contains("오후")) {
            timePreference = "오후";
        } else if (m.contains("저녁") || m.contains("야간")) {
            timePreference = "저녁";
        }

        // 4) 피하고 싶은 요일 (금요일 위주로)
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