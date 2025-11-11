package Hwai_team.UniTime.domain.timetable.service;

import Hwai_team.UniTime.domain.timetable.dto.AiTimetableRequest;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableResponse;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Setter
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
    public Object aiGenerateForUser(Long userId) {
        // 여기를 네가 기존에 사용하던 AI 시간표 생성 로직으로 바꿔줘.
        // 예: 유저의 수강 데이터, 희망 조건 등을 기반으로 timetableResponse 리턴
        System.out.println("AI 시간표 생성 요청 - userId = " + userId);

        // 임시로 예시 데이터 리턴 (테스트용)
        return "예시 시간표: 월요일 자료구조, 화요일 운영체제, 수요일 DB시스템";
    }
}