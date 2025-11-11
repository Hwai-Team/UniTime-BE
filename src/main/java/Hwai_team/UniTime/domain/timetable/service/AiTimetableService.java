// src/main/java/Hwai_team/UniTime/domain/timetable/service/AiTimetableService.java
package Hwai_team.UniTime.domain.timetable.service;

import Hwai_team.UniTime.domain.timetable.dto.AiTimetableRequest;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableResponse;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableSaveRequest;
import Hwai_team.UniTime.domain.timetable.dto.TimetablePlan;
import Hwai_team.UniTime.domain.timetable.entity.AiTimetable;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.timetable.entity.TimetableItem;
import Hwai_team.UniTime.domain.timetable.repository.AiTimetableRepository;
import Hwai_team.UniTime.domain.timetable.repository.TimetableRepository;
import Hwai_team.UniTime.domain.timetable.repository.TimetableItemRepository;
import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.course.repository.CourseRepository;
import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
import Hwai_team.UniTime.global.ai.OpenAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import Hwai_team.UniTime.global.ai.OpenAiClient;

@Service
@RequiredArgsConstructor
public class AiTimetableService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final AiTimetableRepository aiTimetableRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TimetableRepository timetableRepository;
    private final TimetableItemRepository timetableItemRepository;

    @Transactional
    public Timetable createByAi(AiTimetableRequest request) {

        // ✅ 1) 유저 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + request.getUserId()));

        // ✅ 2) 수강 가능 과목 조회 (나중에 학과/학년/학기 필터 추가 가능)
        List<Course> courses = courseRepository.findAll();

        // ✅ 3) 프롬프트 만들기
        String prompt = buildPrompt(user, courses, request.getMessage());

        // ✅ 4) GPT에게서 JSON 문자열 받기
        String json = openAiClient.askTimetableJson(prompt);

        // 🔍 디버그 로그 (일단 System.out으로)
        System.out.println("=== GPT RAW JSON ===");
        System.out.println(json);

        TimetablePlan plan;
        try {
            plan = objectMapper.readValue(json, TimetablePlan.class);
        } catch (Exception e) {
            throw new RuntimeException("시간표 JSON 파싱 실패: " + e.getMessage(), e);
        }

        System.out.println("=== PLAN TITLE ===");
        System.out.println(plan.getTitle());

        if (plan.getItems() == null || plan.getItems().isEmpty()) {
            throw new IllegalStateException("AI가 시간표 아이템을 생성하지 않았습니다. raw=" + json);
        }

        // ✅ title null 방어 + 기본값 설정
        String timetableTitle =
                (plan.getTitle() == null || plan.getTitle().isBlank())
                        ? String.format("%d-%d학기 AI 생성 시간표", request.getYear(), request.getSemester())
                        : plan.getTitle();

        // ✅ 5) 시간표 엔티티 저장
        Timetable timetable = Timetable.builder()
                .owner(user)
                .year(request.getYear())
                .semester(request.getSemester())
                .title(timetableTitle)   // <- 무조건 null 아님
                .build();
        timetableRepository.save(timetable);

        // ✅ 6) 아이템 저장
        for (TimetablePlan.TimetableItemPlan itemPlan : plan.getItems()) {

            TimetableItem item = TimetableItem.builder()
                    .timetable(timetable)
                    .courseName(itemPlan.getCourseName())
                    .dayOfWeek(itemPlan.getDayOfWeek())
                    .startPeriod(toPeriod(itemPlan.getStartTime()))  // "09:00" → 9교시 같은 식
                    .endPeriod(toPeriod(itemPlan.getEndTime()))
                    .room(itemPlan.getLocation())
                    .category(itemPlan.getPriority())
                    .build();

            timetableItemRepository.save(item);
            timetable.addItem(item); // 양방향 연결 유지
        }

        return timetable;
    }

    private String buildPrompt(User user, List<Course> courses, String userMessage) {
        StringBuilder sb = new StringBuilder();

        sb.append("다음 사용자를 위한 대학 시간표를 만들어줘.\n");
        sb.append("사용자 이름: ").append(user.getName()).append("\n");
        sb.append("학과: ").append(user.getDepartment()).append("\n");
        sb.append("학번: ").append(user.getStudentId()).append("\n");
        sb.append("요청 내용: ").append(userMessage).append("\n\n");

        sb.append("수강 가능 과목 리스트:\n");
        for (Course c : courses) {
            sb.append("- ")
                    .append(c.getCourseCode()).append(" / ")
                    .append(c.getName()).append(" / ")
                    .append(c.getCredit()).append("학점 / ")
                    .append(c.getDayOfWeek()).append(" ")
                    .append(c.getStartPeriod()).append("~").append(c.getEndPeriod())
                    .append(" / ")
                    .append(c.getRoom() != null ? c.getRoom() : "")
                    .append("\n");
        }

        sb.append("""
            
            반드시 아래 JSON 구조만 반환해. 다른 텍스트는 절대 쓰지 마.
            {
              "title": "string",
              "items": [
                {
                  "courseCode": "string",
                  "courseName": "string",
                  "dayOfWeek": "MON|TUE|WED|THU|FRI",
                  "startTime": "HH:MM",
                  "endTime": "HH:MM",
                  "location": "string",
                  "priority": "MAJOR|ELECTIVE|OPTIONAL"
                }
              ]
            }
            """);

        return sb.toString();
    }

    // 아주 단순한 변환: "09:00" -> 9
    private int toPeriod(String time) {
        if (time == null || time.isBlank()) return 0;
        try {
            String hour = time.substring(0, 2);
            return Integer.parseInt(hour);
        } catch (Exception e) {
            return 0; // TODO: 나중에 학교 교시 규칙에 맞게 변경
        }
    }

    /**
     * AI 시간표 저장 (유저당 1개만 유지)
     */
    @Transactional
    public AiTimetableResponse saveAiTimetable(AiTimetableSaveRequest request) {

        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (request.getTimetableId() == null) {
            throw new IllegalArgumentException("timetableId는 필수입니다.");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + request.getUserId()));

        Timetable timetable = timetableRepository.findById(request.getTimetableId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시간표입니다. id=" + request.getTimetableId()));

        // 유저당 하나만 관리: 있으면 update, 없으면 새로 생성
        // 유저당 하나만 관리: 있으면 update, 없으면 새로 생성
        AiTimetable aiTimetable = aiTimetableRepository.findByUser_Id(user.getId())
                .orElseGet(() -> AiTimetable.builder()
                        .user(user)
                        .timetable(timetable)
                        .build()
                );

        if (aiTimetable.getId() != null) {
            // ✔ 요약문은 지금 안 쓰니까 null 유지하고, timetable만 교체
            aiTimetable.update(
                    null,       // resultSummary (지금은 안 씀)
                    timetable   // 새로 선택한 시간표
            );
        }

        AiTimetable saved = aiTimetableRepository.save(aiTimetable);
        return AiTimetableResponse.from(saved);
    }

    /**
     * AI 시간표 조회
     */
    @Transactional(readOnly = true)
    public AiTimetableResponse getAiTimetable(Long userId) {
        AiTimetable entity = aiTimetableRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 AI 시간표가 없습니다. userId=" + userId));
        return AiTimetableResponse.from(entity);
    }

    /**
     * AI 시간표 삭제
     */
    @Transactional
    public void deleteAiTimetable(Long userId) {
        aiTimetableRepository.deleteByUser_Id(userId);
    }
}