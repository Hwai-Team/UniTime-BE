package Hwai_team.UniTime.domain.timetable.service;

import Hwai_team.UniTime.domain.timetable.dto.*;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.timetable.repository.TimetableRepository;
import Hwai_team.UniTime.domain.timetable.entity.TimetableItem;
import Hwai_team.UniTime.domain.timetable.repository.TimetableItemRepository;
import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Setter
@Service
@RequiredArgsConstructor
public class TimetableService {

    private final AiTimetableService aiTimetableService;
    private final TimetableItemRepository timetableItemRepository;
    private final TimetableRepository timetableRepository;
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
    /**
     * 내 시간표 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TimetableResponse> getMyTimetables(Long userId) {
        List<Timetable> timetables =
                timetableRepository.findByOwner_IdOrderByYearDescSemesterDesc(userId);

        return timetables.stream()
                .map(TimetableResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 빈 시간표 생성 (연도/학기만 채워진 상태)
     */
    @Transactional
    public TimetableResponse createTimetable(Long userId, TimetableCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + userId));

        if (request.getYear() == null || request.getSemester() == null) {
            throw new IllegalArgumentException("year, semester는 필수입니다.");
        }

        String title = (request.getTitle() == null || request.getTitle().isBlank())
                ? String.format("%d학년도 %d학기", request.getYear(), request.getSemester())
                : request.getTitle();

        Timetable timetable = Timetable.builder()
                .owner(user)
                .year(request.getYear())
                .semester(request.getSemester())
                .title(title)
                .build();

        Timetable saved = timetableRepository.save(timetable);
        return TimetableResponse.from(saved);
    }

    /**
     * 시간표 수정 (제목 + 아이템 전체 교체)
     */
    @Transactional
    public TimetableResponse updateTimetable(Long timetableId, TimetableUpdateRequest request) {

        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시간표입니다. id=" + timetableId));

        // TimetableService.java 중 일부
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            timetable.changeTitle(request.getTitle());  // ✅ setTitle → changeTitle로 수정
        }

        // 기존 아이템 싹 다 삭제 후, 새로 다 넣는 방식 (프론트가 현재 상태 전체를 보내준다는 가정)
        timetableItemRepository.deleteByTimetable_Id(timetableId);
        timetable.getItems().clear();

        if (request.getItems() != null) {
            for (TimetableItemDto dto : request.getItems()) {
                TimetableItem item = TimetableItem.builder()
                        .timetable(timetable)
                        .courseName(dto.getCourseName())
                        .dayOfWeek(dto.getDayOfWeek())
                        .startPeriod(dto.getStartPeriod())
                        .endPeriod(dto.getEndPeriod())
                        .room(dto.getRoom())
                        .category(dto.getCategory())
                        .build();
                timetableItemRepository.save(item);
                timetable.addItem(item);  // 양방향 연관관계 편의 메서드 있으면 사용
            }
        }

        return TimetableResponse.from(timetable);
    }

    /**
     * 시간표 삭제
     */
    @Transactional
    public void deleteTimetable(Long timetableId) {
        // 아이템 먼저 삭제
        timetableItemRepository.deleteByTimetable_Id(timetableId);
        // 시간표 삭제
        timetableRepository.deleteById(timetableId);
    }
}