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

    /**
     * ✅ AI 시간표 생성
     * - 프론트에서 body로 userId, message, year, semester를 보내줌
     * - 여기서 AiTimetableService.createByAi() 호출해서 Timetable 생성
     * - 생성된 Timetable을 TimetableResponse 형태로 바로 반환 (FE의 AIGenerateTimetableResponse와 구조 호환)
     */
    @Transactional
    public TimetableResponse generateAiTimetable(AiTimetableRequest request) {

        Long userId = request.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + userId));

        // AI 시간표 실제 생성 (여기서 AiTimetable 엔티티까지 같이 관리)
        Timetable timetable = aiTimetableService.createByAi(request);

        // 프론트가 원하는 건 '방금 만들어진 시간표 내용'
        return TimetableResponse.from(timetable);
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

    /**
     * ✅ 시간표 수정 (제목 + 아이템 전체 교체)
     * - 이미지에서 가져온 강의들은 courseId가 없을 수 있으므로
     *   courseId가 null인 경우 Course를 조회하지 않고, 이름/요일/교시만으로 저장 허용
     */
    @Transactional
    public TimetableResponse updateTimetable(Long timetableId, TimetableUpdateRequest request) {

        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시간표입니다. id=" + timetableId));

        // 제목 변경
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            timetable.changeTitle(request.getTitle());
        }

        // 아이템 전체 교체 모드
        if (request.getItems() != null) {
            // 1) 기존 아이템 DB에서 삭제
            timetableItemRepository.deleteByTimetable_Id(timetableId);

            // 2) 엔티티 내부 컬렉션도 정리 (양방향 일관성 유지)
            if (timetable.getItems() != null) {
                timetable.getItems().clear();
            }

            // 3) 요청 아이템으로 새로 구성
            for (TimetableUpdateRequest.Item it : request.getItems()) {

                // ✅ courseId가 있을 때만 Course 조회
                Course course = null;
                if (it.getCourseId() != null) {
                    course = courseRepository.findById(it.getCourseId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과목입니다. id=" + it.getCourseId()));
                }

                // ✅ 우선순위: 요청값 > DB(course) 값
                String dayOfWeek = it.getDayOfWeek();
                Integer start    = it.getStartPeriod();
                Integer end      = it.getEndPeriod();
                String room      = it.getRoom();
                String category  = it.getCategory();
                String courseName = it.getCourseName();

                if (course != null) {
                    if (dayOfWeek == null) dayOfWeek = course.getDayOfWeek();
                    if (start == null)     start     = course.getStartPeriod();
                    if (end == null)       end       = course.getEndPeriod();
                    if (room == null)      room      = course.getRoom();
                    if (category == null)  category  = course.getCategory();
                    if (courseName == null || courseName.isBlank()) {
                        courseName = course.getName();
                    }
                }

                // 필수 컬럼 검증 (이미지로 들어온 애들은 request 쪽에 값이 채워져 있어야 함)
                if (dayOfWeek == null || start == null || end == null) {
                    throw new IllegalArgumentException(
                            "시간표 아이템의 시간정보가 부족합니다. " +
                                    "dayOfWeek/startPeriod/endPeriod는 최소 한쪽(DB 또는 요청)에서 채워져야 합니다."
                    );
                }

                // courseName도 비어 있으면 그냥 "이름 없는 강의" 꼴이라 웬만하면 막는 게 낫다
                if (courseName == null || courseName.isBlank()) {
                    throw new IllegalArgumentException(
                            "과목 이름(courseName)이 없습니다. 이미지에서 불러온 항목이라면 courseName을 채워서 보내주세요."
                    );
                }

                TimetableItem item = TimetableItem.builder()
                        .timetable(timetable)
                        .course(course)        // ✅ 이미지에서 온 건 null일 수 있음
                        .courseName(courseName)
                        .category(category)
                        .dayOfWeek(dayOfWeek)
                        .startPeriod(start)
                        .endPeriod(end)
                        .room(room)
                        .build();

                // 저장 + timetable에 추가 (여기서 충돌 체크 등 수행)
                timetableItemRepository.save(item);
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