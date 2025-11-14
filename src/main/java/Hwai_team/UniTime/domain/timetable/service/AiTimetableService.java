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

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiTimetableService {

    private static final int MAX_CREDITS = 18;

    private final ObjectMapper objectMapper; // 확장 대비(요약 등)
    private final AiTimetableRepository aiTimetableRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TimetableRepository timetableRepository;
    private final TimetableItemRepository timetableItemRepository;

    /** DB 기반 생성: 재수강(무조건) → 전공(학과/학년 우선, 완화 fallback) → 교양(교필/교선) */
    @Transactional
    public Timetable createByAi(AiTimetableRequest request) {
        // 1) 유저
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + request.getUserId()));
        final String userDept = normDept(user.getDepartment());
        final Integer userGrade = user.getGrade();
        final String message = nullToEmpty(request.getMessage());

        // 2) 선호 파싱
        final Integer maxDays = parseMaxDays(message).orElse(null);      // "주 3일"
        final boolean avoidFirstPeriod = detectAvoidFirstPeriod(message); // 1교시 피하기

        // 3) 전체 과목
        List<Course> all = courseRepository.findAll();

        // 4) 재수강 후보(메시지 내 이름/코드 포함)
        List<Course> retake = pickRetakeCourses(message, all);

        // 5) 전공 후보(엄격)
        List<Course> majorStrict = all.stream()
                .filter(this::isMajorCategory)
                .filter(c -> deptStrictMatch(userDept, normDept(c.getDepartment())))
                .filter(c -> recGradeStrictMatch(userGrade, c.getRecommendedGrade()))
                .collect(Collectors.toList());

        // 6) 전공 후보(완화, 엄격 결과가 비었을 때만)
        List<Course> majorRelaxed = majorStrict.isEmpty()
                ? all.stream()
                .filter(this::isMajorCategory)
                .filter(c -> recGradeRelaxedMatch(userGrade, c.getRecommendedGrade()))
                .filter(c -> deptRelaxedMatch(userDept, normDept(c.getDepartment())))
                .collect(Collectors.toList())
                : Collections.emptyList();

        // 최종 전공 목록
        List<Course> major = majorStrict.isEmpty() ? majorRelaxed : majorStrict;

        // 7) 교양 후보
        List<Course> liberal = all.stream()
                .filter(this::isLiberalCategory)
                .collect(Collectors.toList());

        // 8) 시간표 엔티티
        Timetable timetable = Timetable.builder()
                .owner(user)
                .year(request.getYear())
                .semester(request.getSemester())
                .title(buildTitle(user, request))
                .build();
        timetableRepository.save(timetable);

        int totalCredits = 0;
        Set<String> usedCourseCodes = new HashSet<>();
        Map<String, List<int[]>> occupied = new HashMap<>(); // day -> [start,end)
        Set<String> usedDays = new HashSet<>();

        CourseAdder adder = new CourseAdder(
                timetable, timetableItemRepository, usedCourseCodes, occupied, usedDays
        );

        // 정렬: 전공은 시간 빠른 순(1교시 회피 X), 교양은 1교시 회피 옵션 반영
        major.sort(byStartPeriodWithAvoid(false));
        liberal.sort(byStartPeriodWithAvoid(avoidFirstPeriod));

        // 8-1) 재수강: 충돌/요일제한 무시하고 우선 포함(학점만 체크)
        totalCredits = adder.addCourses(retake, totalCredits, MAX_CREDITS, maxDays,
                false, /*applyFirstPeriodFilter*/
                true,  /*forceAddRetake*/
                true   /*ignoreDayLimit*/
        );

        // 8-2) 전공: 충돌 체크, 요일 제한 무시, 1교시 회피 미적용(전공 우선)
        major = major.stream()
                .filter(c -> !usedCourseCodes.contains(nullToEmpty(c.getCourseCode())))
                .collect(Collectors.toList());
        totalCredits = adder.addCourses(major, totalCredits, MAX_CREDITS, maxDays,
                false, /*applyFirstPeriodFilter*/
                false, /*forceAddRetake*/
                true   /*ignoreDayLimit*/
        );

        // 8-3) 교양: 충돌/요일/1교시 회피 모두 적용
        liberal = liberal.stream()
                .filter(c -> !usedCourseCodes.contains(nullToEmpty(c.getCourseCode())))
                .collect(Collectors.toList());
        totalCredits = adder.addCourses(liberal, totalCredits, MAX_CREDITS, maxDays,
                avoidFirstPeriod,
                false,
                false
        );

        // 9) AiTimetable 기록(유저당 1개)
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

    /** 전공 카테고리 여부(전*, 전공*) */
    private boolean isMajorCategory(Course c) {
        String cat = nullToEmpty(c.getCategory());
        return cat.startsWith("전") || cat.contains("전공");
    }

    /** 교양 카테고리 여부(교필/교선) */
    private boolean isLiberalCategory(Course c) {
        String cat = nullToEmpty(c.getCategory());
        return cat.equals("교필") || cat.equals("교선");
    }

    /** 학과 엄격 매칭(정규화 후 완전일치) */
    private boolean deptStrictMatch(String userDept, String courseDept) {
        if (userDept.isEmpty() || courseDept.isEmpty()) return false;
        return userDept.equals(courseDept);
    }

    /** 학과 완화 매칭(부분포함/토큰 겹침 허용) */
    private boolean deptRelaxedMatch(String userDept, String courseDept) {
        if (courseDept.isEmpty()) return true; // 과목 학과 미지정이면 허용
        if (userDept.isEmpty()) return false;

        // 부분 포함
        if (userDept.contains(courseDept) || courseDept.contains(userDept)) return true;

        // 토큰 기반(한글/영문/숫자 연속 토큰)
        Set<String> uTokens = deptTokens(userDept);
        Set<String> cTokens = deptTokens(courseDept);
        uTokens.retainAll(cTokens);
        return !uTokens.isEmpty();
    }

    private Set<String> deptTokens(String s) {
        s = s.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ").trim();
        if (s.isEmpty()) return new HashSet<>();
        return Arrays.stream(s.split("\\s+"))
                .filter(t -> t.length() >= 2)
                .collect(Collectors.toSet());
    }

    /** 추천학년 엄격: null 허용 또는 정확히 일치 */
    private boolean recGradeStrictMatch(Integer userGrade, Integer rec) {
        return rec == null || userGrade == null || rec.equals(userGrade);
    }

    /** 추천학년 완화: null 허용 또는 |diff| ≤ 1 */
    private boolean recGradeRelaxedMatch(Integer userGrade, Integer rec) {
        if (rec == null || userGrade == null) return true;
        return Math.abs(rec - userGrade) <= 1;
    }

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
                    return cat.startsWith("전") || cat.contains("전공");
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
                if (sa == 1 && sb != 1) return 1;
                if (sb == 1 && sa != 1) return -1;
            }
            return Integer.compare(sa, sb);
        };
    }

    /** 재수강 후보 탐색: 이름/코드가 메시지에 포함되면 픽업 (공백 제거/소문자 비교) */
    private static List<Course> pickRetakeCourses(String message, List<Course> all) {
        String msg = normalize(message);
        boolean cue = msg.contains("재수강") || msg.contains("재수") ||
                msg.contains("retake") || msg.contains("repeat") ||
                msg.contains("꼭들") || msg.contains("반드시") || msg.contains("다시들");
        if (!cue) return new ArrayList<>();

        List<Course> list = new ArrayList<>();
        for (Course c : all) {
            String name = normalize(c.getName());
            String code = normalize(c.getCourseCode());
            if ((!name.isEmpty() && containsToken(msg, name)) || (!code.isEmpty() && msg.contains(code))) {
                list.add(c);
            }
        }
        return list;
    }

    private static String normalize(String s) {
        s = nullToEmpty(s).toLowerCase().replaceAll("\\s+", "");
        // 한글 정규화(NFC)
        return Normalizer.normalize(s, Normalizer.Form.NFC);
    }
    private static boolean containsToken(String hay, String needle) {
        if (needle.length() < 3) return false; // 너무 짧은 토큰 방지
        return hay.contains(needle);
    }

    private static String normDept(String s) {
        s = nullToEmpty(s);
        s = s.replaceAll("\\s+", "");
        return Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
    private static int safeInt(Integer i) {
        return i == null ? 0 : i;
    }

    private static Optional<Integer> parseMaxDays(String msg) {
        Matcher m = Pattern.compile("주\\s*(\\d)\\s*일").matcher(msg);
        if (m.find()) {
            try { return Optional.of(Integer.parseInt(m.group(1))); }
            catch (NumberFormatException ignored) {}
        }
        return Optional.empty();
    }

    private static boolean detectAvoidFirstPeriod(String msg) {
        String m = msg.replaceAll("\\s+", "");
        return (m.contains("1교시") || m.contains("첫교시")) &&
                (m.contains("피") || m.contains("빼") || m.contains("안") || m.contains("없"));
    }

    /** 내부 헬퍼: 과목 추가(충돌/요일/학점/선호/중복) 처리 */
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
                       boolean applyFirstPeriodFilter,
                       boolean forceAddRetake,
                       boolean ignoreDayLimit) {

            for (Course c : candidates) {
                if (currentCredits >= maxCredits) break;

                String code = nullToEmpty(c.getCourseCode());
                if (!code.isEmpty() && usedCodes.contains(code)) continue;

                String day = nullToEmpty(c.getDayOfWeek());
                int start = safeInt(c.getStartPeriod());
                int end = safeInt(c.getEndPeriod());
                int endExclusive = end + 1;

                // 1교시 회피(재수강/전공엔 적용 안 하도록 외부에서 false로 전달)
                if (applyFirstPeriodFilter && start == 1) continue;

                // 요일 제한(재수강/전공은 무시 가능)
                if (!ignoreDayLimit && maxDays != null) {
                    boolean newDay = !usedDays.contains(day);
                    if (newDay && (usedDays.size() + 1) > maxDays) {
                        continue;
                    }
                }

                // 충돌 체크
                boolean conflict = isConflict(day, start, endExclusive);
                if (conflict && !forceAddRetake) continue;

                // 학점 초과 방지
                int after = currentCredits + safeInt(c.getCredit());
                if (after > maxCredits) continue;

                // 저장
                TimetableItem item = TimetableItem.builder()
                        .timetable(timetable)
                        .course(c) // FK 저장 → DTO에서 courseId 제공 가능
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
            if (day.isEmpty()) return false;
            List<int[]> list = occupied.getOrDefault(day, new ArrayList<>());
            for (int[] r : list) {
                int as = r[0], ae = r[1];
                if (start < ae && as < endExclusive) return true; // overlap
            }
            return false;
        }

        private void occupy(String day, int start, int endExclusive) {
            if (day.isEmpty()) return;
            usedDays.add(day);
            occupied.computeIfAbsent(day, k -> new ArrayList<>()).add(new int[]{start, endExclusive});
        }
    }
}