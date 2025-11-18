// src/main/java/Hwai_team/UniTime/domain/timetable/service/TimetableService.java
package Hwai_team.UniTime.domain.timetable.service;

import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.course.repository.CourseRepository;
import Hwai_team.UniTime.domain.timetable.dto.*;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.timetable.entity.TimetableItem;
import Hwai_team.UniTime.domain.timetable.repository.TimetableItemRepository;
import Hwai_team.UniTime.domain.timetable.repository.TimetableRepository;
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
    private final CourseRepository courseRepository;

    /** AI 시간표 생성 위임 */
    @Transactional
    public AiTimetableResponse generateAiTimetable(Long userId, AiTimetableRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId));

        Timetable timetable = aiTimetableService.createByAi(request);

        AiTimetableResponse response = new AiTimetableResponse();
        response.setTimetableId(timetable.getId());
        response.setTitle(timetable.getTitle());
        response.setUserName(user.getName());
        response.setMessage("AI 시간표가 성공적으로 생성되었습니다.");
        return response;
    }

    /** 내 시간표 목록 조회 */
    @Transactional(readOnly = true)
    public List<TimetableResponse> getMyTimetables(Long userId) {
        List<Timetable> timetables = timetableRepository.findByOwner_IdOrderByYearDescSemesterDesc(userId);
        return timetables.stream().map(TimetableResponse::from).collect(Collectors.toList());
    }

    /** 빈 시간표 생성 */
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

    /** 시간표 수정 (제목 + 아이템 전체 교체) */
    @Transactional
    public TimetableResponse updateTimetable(Long timetableId, TimetableUpdateRequest request) {

        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시간표입니다. id=" + timetableId));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            timetable.changeTitle(request.getTitle());
        }


        if (request.getItems() != null) {
            for (TimetableUpdateRequest.Item it : request.getItems()) {
                Course course = courseRepository.findById(it.getCourseId())
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과목입니다. id=" + it.getCourseId()));

                // 요청에 없으면 DB 값으로 대체
                String dayOfWeek   = (it.getDayOfWeek()   != null) ? it.getDayOfWeek()   : course.getDayOfWeek();
                Integer start      = (it.getStartPeriod() != null) ? it.getStartPeriod() : course.getStartPeriod();
                Integer end        = (it.getEndPeriod()   != null) ? it.getEndPeriod()   : course.getEndPeriod();
                String room        = (it.getRoom()        != null) ? it.getRoom()        : course.getRoom();

                // 필수 컬럼 검증(테이블이 NOT NULL이면 여기서 잡아줌)
                if (dayOfWeek == null || start == null || end == null) {
                    throw new IllegalArgumentException(
                            "과목(" + course.getName() + ")의 시간정보가 부족합니다. " +
                                    "DB(course) 값 또는 요청(dayOfWeek/startPeriod/endPeriod) 중 하나는 채워져야 합니다."
                    );
                }

                TimetableItem item = TimetableItem.builder()
                        .timetable(timetable)
                        .courseName(course.getName())
                        .category(course.getCategory())
                        .dayOfWeek(dayOfWeek)
                        .startPeriod(start)
                        .endPeriod(end)
                        .room(room)
                        .build();

                timetable.addItem(item);
            }
        }

        return TimetableResponse.from(timetable);
    }

    /** 시간표 삭제 */
    @Transactional
    public void deleteTimetable(Long timetableId) {
        timetableItemRepository.deleteByTimetable_Id(timetableId);
        timetableRepository.deleteById(timetableId);
    }
}