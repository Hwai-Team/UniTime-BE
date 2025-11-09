package Hwai_team.UniTime.domain.timetable.service;

import Hwai_team.UniTime.domain.timetable.dto.AiTimetableRequest;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableResponse;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final AiTimetableService aiTimetableService;
    private final UserRepository userRepository;

    /**
     * AI 시간표 생성 요청을 위임
     */
    @Transactional
    public AiTimetableResponse generateAiTimetable(Long userId, AiTimetableRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId));

        // AiTimetableService 사용해서 실제 생성
        Timetable timetable = aiTimetableService.createByAi(request);

        // 응답 DTO 구성
        AiTimetableResponse response = new AiTimetableResponse();
        response.setTimetableId(timetable.getId());
        response.setTitle(timetable.getTitle());
        response.setUserName(user.getName());
        response.setMessage("AI 시간표가 성공적으로 생성되었습니다.");

        return response;
    }
}