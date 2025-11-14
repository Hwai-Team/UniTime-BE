// src/main/java/Hwai_team/UniTime/domain/timetable/service/AiTimetableService.java
package Hwai_team.UniTime.domain.timetable.service;

import Hwai_team.UniTime.domain.timetable.dto.AiTimetableRequest;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableResponse;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableSaveRequest;
import Hwai_team.UniTime.domain.timetable.entity.AiTimetable;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.timetable.entity.TimetableItem;
import Hwai_team.UniTime.domain.timetable.repository.AiTimetableRepository;
import Hwai_team.UniTime.domain.timetable.repository.TimetableItemRepository;
import Hwai_team.UniTime.domain.timetable.repository.TimetableRepository;
import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.course.repository.CourseRepository;
import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiTimetableService {

    private static final int MAX_CREDITS = 18;

    private final ObjectMapper objectMapper; // 현재는 사용 안 하지만, 이후 요약 등 필요시 대비
    private final AiTimetableRepository aiTimetableRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TimetableRepository timetableRepository;
    private final TimetableItemRepository timetableItemRepository;

    /**
     * GPT를 쓰지 않고, DB의 과목만으로 시간표를 구성한다.
     * 순서: 재수강 → 전공(학과+학년) → 교양(교필/교선), 최대 18학점.
     * 주 N일/1교시 회피 등 간단한 선호도 반영, 요일/교시 충돌 자동 회피.
     */
    @Transactional
    public Timetable createByAi(AiTimetableRequest request) {
        // 1) 유저
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + request.getUserId()));
        final String department = nullToEmpty(user.getDepartment());
        final Integer grade = user.getGrade();

        final String message = nullToEmpty(request.getMessage());

        // 2) 선호도 파싱
        final Integer maxDays = parseMaxDays(message).orElse(null);  // 예: "주 3일"
        final boolean avoidFirstPeriod = detectAvoidFirstPeriod(message); // "1교시 피..."

        // 3) 전체 과목 로드 (필요 시 search()로 대체 가능)
        List<Course> all = courseRepository.findAll();

        // 4) 재수강 후보: 메시지에 언급된 과목명/코드가 포함된 과목
        List<Course> retake = pickRetakeCourses(message, all);

        // 5) 전공 후보: 학과/학년이 정확히 일치하는 과목
        List<Course> major = all.stream()
                .filter(c -> equalsIgnoreCase(nullToEmpty(c.getDepartment()), department))
                .filter(c -> c.getRecommendedGrade() != null && grade != null && c.getRecommendedGrade().equals(grade))
                .collect(Collectors.toList());

        // 6) 교양 후보: category ∈ {교필, 교선}
        List<Course> liberal = all.stream()
                .filter(c -> {
                    String cat = nullToEmpty(c.getCategory());
                    return cat.equals("교필") || cat.equals("교선");
                })
                .collect(Collectors.toList());

        // 7) 빌드: 재수강 → 전공 → 교양
        Timetable timetable = Timetable.builder()
                .owner(user)
                .year(request.getYear())
                .semester(request.getSemester())
                .title(buildTitle(user, request))
                .build();
        timetableRepository.save(timetable);

        // 상태
        int totalCredits = 0;
        Set<String> usedCourseCodes = new HashSet<>();
        Map<String, List<int[]>> occupied = new HashMap<>(); // day -> list of [start,end] (end exclusive)
        Set<String> usedDays = new HashSet<>();

        // 내부 헬퍼
        final CourseAdder adder = new CourseAdder(
                timetable, timetableItemRepository, usedCourseCodes, occupied, usedDays
        );

        // 7-1) 재수강 먼저
        totalCredits = adder.addCourses(retake, totalCredits, MAX_CREDITS, maxDays, avoidFirstPeriod, true);

        // 7-2) 전공(중복/충돌/학점/요일 제한 고려)
        // 재수강에서 이미 담은 코드 제거
        major = major.stream().filter(c -> !usedCourseCodes.contains(nullToEmpty(c.getCourseCode()))).collect(Collectors.toList());
        // 1교시 회피가 있으면 시작교시가 1인 과목들을 뒤로 밀기
        major.sort(byStartPeriodWithAvoid(avoidFirstPeriod));
        totalCredits = adder.addCourses(major, totalCredits, MAX_CREDITS, maxDays, avoidFirstPeriod, false);

        // 7-3) 교양(교필/교선)
        liberal = liberal.stream().filter(c -> !usedCourseCodes.contains(nullToEmpty(c.getCourseCode()))).collect(Collectors.toList());
        liberal.sort(byStartPeriodWithAvoid(avoidFirstPeriod));
        totalCredits = adder.addCourses(liberal, totalCredits, MAX_CREDITS, maxDays, avoidFirstPeriod, false);

        // 8) AiTimetable 기록(유저당 1개) — 프롬프트 대신 사용자 메시지를 보관
        AiTimetable aiTimetable = aiTimetableRepository.findByUser_Id(user.getId())
                .orElseGet(() -> AiTimetable.builder()
                        .user(user)
                        .prompt(message)
                        .timetable(timetable)
                        .build()
                );
        aiTimetable.update(buildResultSummary(timetable), timetable);
        aiTimetableRepository.save(aiTimetable);

        return timetable;
    }

    /** (옵션) 수동 저장/수정 */
    @Transactional
    public AiTimetableResponse saveAiTimetable(AiTimetableSaveRequest request) {
        if (request.getUserId() == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (request.getTimetableId() == null) throw new IllegalArgumentException("timetableId는 필수입니다.");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        Timetable timetable = timetableRepository.findById(request.getTimetableId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시간표입니다."));

        AiTimetable aiTimetable = aiTimetableRepository.findByUser_Id(user.getId())
                .orElseGet(() -> AiTimetable.builder()
                        .user(user)
                        .timetable(timetable)
                        .build()
                );

        aiTimetable.update(request.getResultSummary(), timetable);
        return AiTimetableResponse.from(aiTimetableRepository.save(aiTimetable));
    }

    /** AI 시간표 조회 */
    @Transactional(readOnly = true)
    public AiTimetableResponse getAiTimetable(Long userId) {
        AiTimetable entity = aiTimetableRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 AI 시간표가 없습니다."));
        return AiTimetableResponse.from(entity);
    }

    /** AI 시간표 삭제 */
    @Transactional
    public void deleteAiTimetable(Long userId) {
        aiTimetableRepository.deleteByUser_Id(userId);
    }

    // =======================
    // 내부 유틸 / 헬퍼
    // =======================

    private static String buildTitle(User user, AiTimetableRequest req) {
        String dep = nullToEmpty(user.getDepartment());
        Integer g = user.getGrade();
        if (dep.isEmpty() && g == null) {
            return (req.getYear() != null && req.getSemester() != null)
                    ? String.format("%d-%d학기 AI 생성 시간표", req.getYear(), req.getSemester())
                    : "AI 생성 시간표";
        }
        return String.format("%s의 %s%s 시간표",
                nullToEmpty(user.getName()),
                g != null ? (g + "학년 ") : "",
                dep.isEmpty() ? "" : dep
        ).trim();
    }

    private static String buildResultSummary(Timetable timetable) {
        int credits = timetable.getItems() == null ? 0 :
                timetable.getItems().stream().mapToInt(i -> i.getCourse() != null ? i.getCourse().getCredit() : 0).sum();
        long majorCnt = timetable.getItems() == null ? 0 :
                timetable.getItems().stream().filter(i -> {
                    String cat = nullToEmpty(i.getCategory());
                    return cat.contains("전공");
                }).count();
        long liberalCnt = timetable.getItems() == null ? 0 :
                timetable.getItems().stream().filter(i -> {
                    String cat = nullToEmpty(i.getCategory());
                    return cat.equals("교필") || cat.equals("교선");
                }).count();

        return String.format("전공 %d개, 교양 %d개, 총 %d학점", majorCnt, liberalCnt, credits);
    }

    private static Comparator<Course> byStartPeriodWithAvoid(boolean avoidFirstPeriod) {
        return (a, b) -> {
            int sa = safeInt(a.getStartPeriod());
            int sb = safeInt(b.getStartPeriod());
            if (avoidFirstPeriod) {
                // 시작교시가 1인 과목은 뒤로
                if (sa == 1 && sb != 1) return 1;
                if (sb == 1 && sa != 1) return -1;
            }
            return Integer.compare(sa, sb);
        };
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a.equalsIgnoreCase(b);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static int safeInt(Integer i) {
        return i == null ? 0 : i;
    }

    private static Optional<Integer> parseMaxDays(String msg) {
        // "주 3일", "주3일", "주 2일만" 등
        Matcher m = Pattern.compile("주\\s*(\\d)\\s*일").matcher(msg);
        if (m.find()) {
            try {
                return Optional.of(Integer.parseInt(m.group(1)));
            } catch (NumberFormatException ignored) {}
        }
        return Optional.empty();
    }

    private static boolean detectAvoidFirstPeriod(String msg) {
        // "1교시 피", "1교시는 피", "첫 교시 피", 등
        String m = msg.replaceAll("\\s+", "");
        return (m.contains("1교시") || m.contains("첫교시")) && (m.contains("피") || m.contains("빼") || m.contains("안") || m.contains("없"));
    }

    private static List<Course> pickRetakeCourses(String message, List<Course> all) {
        String lower = message.toLowerCase();
        boolean hasRetakeCue = lower.contains("재수강") || lower.contains("재수") ||
                lower.contains("retake") || lower.contains("repeat") || lower.contains("다시들");
        if (!hasRetakeCue) {
            // 그래도 '꼭 들어야 해'면 포함
            hasRetakeCue = lower.contains("꼭들") || lower.contains("반드시");
        }

        if (!hasRetakeCue) return new ArrayList<>();

        // 과목명이 메시지에 등장하거나, 코스코드가 등장하면 재수강으로 간주
        List<Course> list = new ArrayList<>();
        for (Course c : all) {
            String name = nullToEmpty(c.getName());
            String code = nullToEmpty(c.getCourseCode());
            if (name.isEmpty() && code.isEmpty()) continue;

            if ((!name.isEmpty() && lower.contains(name.toLowerCase()))
                    || (!code.isEmpty() && lower.contains(code.toLowerCase()))) {
                list.add(c);
            }
        }
        return list;
    }

    /** 내부 헬퍼: 과목 추가 로직(충돌/요일/학점/회피/중복 모두 처리) */
    private static class CourseAdder {
        private final Timetable timetable;
        private final TimetableItemRepository repo;
        private final Set<String> usedCodes;
        private final Map<String, List<int[]>> occupied; // day -> [start,end)
        private final Set<String> usedDays;

        private CourseAdder(Timetable timetable,
                            TimetableItemRepository repo,
                            Set<String> usedCodes,
                            Map<String, List<int[]>> occupied,
                            Set<String> usedDays) {
            this.timetable = timetable;
            this.repo = repo;
            this.usedCodes = usedCodes;
            this.occupied = occupied;
            this.usedDays = usedDays;
        }

        int addCourses(List<Course> candidates,
                       int currentCredits,
                       int maxCredits,
                       Integer maxDays,
                       boolean avoidFirstPeriod,
                       boolean forceAddRetake) {

            for (Course c : candidates) {
                if (currentCredits >= maxCredits) break;

                String code = nullToEmpty(c.getCourseCode());
                if (!code.isEmpty() && usedCodes.contains(code)) continue;

                String day = nullToEmpty(c.getDayOfWeek());
                int start = safeInt(c.getStartPeriod());
                int end = safeInt(c.getEndPeriod());
                int endExclusive = end + 1; // half-open

                // 1교시 회피
                if (!forceAddRetake && avoidFirstPeriod && start == 1) continue;

                // 요일 제한: 새 요일 추가 시 maxDays 초과하면 스킵
                if (maxDays != null) {
                    boolean newDay = !usedDays.contains(day);
                    if (newDay && (usedDays.size() + 1) > maxDays) {
                        // 새날로 확장하지 않고 기존 사용 요일 위주로 담기
                        // → 이 과목은 스킵
                        continue;
                    }
                }

                // 충돌 체크
                if (isConflict(day, start, endExclusive)) {
                    if (forceAddRetake) {
                        // 재수강은 강제로 넣고, 같은 시간 다른 과목을 넣지 않도록 점유만 한다.
                        occupy(day, start, endExclusive);
                    } else {
                        continue; // 충돌이면 스킵
                    }
                }

                // 학점 초과 방지
                int after = currentCredits + safeInt(c.getCredit());
                if (after > maxCredits) continue;

                // 저장
                TimetableItem item = TimetableItem.builder()
                        .timetable(timetable)
                        .course(c) // FK 저장 -> 응답 DTO에서 courseId 가능
                        .courseName(c.getName())
                        .dayOfWeek(c.getDayOfWeek())
                        .startPeriod(c.getStartPeriod())
                        .endPeriod(c.getEndPeriod())
                        .room(c.getRoom())
                        .category(c.getCategory())
                        .build();
                repo.save(item);
                timetable.addItem(item);

                // 상태 갱신
                currentCredits = after;
                if (!code.isEmpty()) usedCodes.add(code);
                occupy(day, start, endExclusive);
            }
            return currentCredits;
        }

        private boolean isConflict(String day, int start, int endExclusive) {
            if (day.isEmpty()) return false; // 요일 정보 없으면 충돌 판정 생략
            List<int[]> list = occupied.getOrDefault(day, new ArrayList<>());
            for (int[] r : list) {
                int as = r[0], ae = r[1];
                // overlap if: start < ae && as < end
                if (start < ae && as < endExclusive) {
                    return true;
                }
            }
            return false;
        }

        private void occupy(String day, int start, int endExclusive) {
            if (day.isEmpty()) return;
            usedDays.add(day);
            List<int[]> list = occupied.computeIfAbsent(day, k -> new ArrayList<>());
            list.add(new int[]{start, endExclusive});
        }
    }
}