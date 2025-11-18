package Hwai_team.UniTime.domain.timetable.service;

import Hwai_team.UniTime.domain.chat.entity.ChatMessage;
import Hwai_team.UniTime.domain.chat.repository.ChatMessageRepository;
import Hwai_team.UniTime.domain.timetable.dto.AiGenerateButtonRequest;
import Hwai_team.UniTime.domain.timetable.dto.AiGenerateButtonResponse;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.timetable.entity.TimetableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableAiIntentService {

    private final ChatMessageRepository chatMessageRepository;

    public AiGenerateButtonResponse checkButtonVisibility(AiGenerateButtonRequest request) {
        String text = request.lastUserMessage().toLowerCase();

        // 1차: 키워드 기반 간단 룰 (의도 감지)
        boolean hit = text.contains("시간표") &&
                (text.contains("만들") || text.contains("추천") || text.contains("짜줘"));

        if (!hit) {
            return new AiGenerateButtonResponse(
                    false,
                    "키워드 매칭 실패",
                    null,
                    null
            );
        }

        // ✅ 2차: 의도 감지 성공 시, 유저 전체 대화 모으기
        List<ChatMessage> messages =
                chatMessageRepository.findByUser_IdOrderByCreatedAtAsc(request.userId());

        // 유저가 말한 것만 골라서 한 덩어리로
        String userOnlyConversation = messages.stream()
                .filter(m -> "USER".equals(m.getRole()))
                .map(ChatMessage::getContent)
                .map(s -> s.replace("\n", " "))  // 줄바꿈 정리
                .collect(Collectors.joining("\n- ", "- ", ""));

        // ✅ 시간표 생성에 그대로 넘길 “정리된 메시지”
        String timetableContextMsg = """
                아래는 유저가 지금까지 챗봇에게 말한 내용이야.
                이 정보를 기반으로 이번 학기 시간표를 짤 때 필요한 조건들을 파악해서 시간표를 생성해.

                [유저 발화 모음]
                %s
                """.formatted(userOnlyConversation);

        String suggestion = "AI로 이번 학기 시간표를 자동으로 생성해볼까요?";

        return new AiGenerateButtonResponse(
                true,
                "키워드 매칭 성공",
                suggestion,
                timetableContextMsg
        );
    }
    public interface TimetableItemRepository extends JpaRepository<TimetableItem, Long> {

        boolean existsByTimetableAndDayOfWeekAndStartPeriodLessThanEqualAndEndPeriodGreaterThanEqual(
                Timetable timetable,
                String dayOfWeek,
                Integer endPeriod,
                Integer startPeriod
        );
    }
}