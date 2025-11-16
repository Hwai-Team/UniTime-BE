package Hwai_team.UniTime.domain.timetable.service;

import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.course.repository.CourseRepository;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableRequest;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableResponse;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableSaveRequest;
import Hwai_team.UniTime.domain.timetable.dto.TimetableSummaryResponse;
import Hwai_team.UniTime.domain.timetable.entity.AiTimetable;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.timetable.entity.TimetableItem;
import Hwai_team.UniTime.domain.timetable.repository.AiTimetableRepository;
import Hwai_team.UniTime.domain.timetable.repository.TimetableItemRepository;
import Hwai_team.UniTime.domain.timetable.repository.TimetableRepository;
import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
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
    private static final int MAX_MAJOR_COUNT = 5; // 전공 최대 개수

    private final AiTimetableRepository aiTimetableRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TimetableRepository timetableRepository;
    private final TimetableItemRepository timetableItemRepository;

    /**
     * title: AI 시간표 생성 서비스
     * 재수강(우선) → 전공(최대 5개, 학년 정확 일치) → 교양(교필/교선)
     * creator: 김민호
     * @param: userId, message, year, semester
     */
    @Transactional
    public Timetable createByAi(AiTimetableRequest request) {
        // 1) 유저
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + request.getUserId()));
        final String userDept = normDept(user.getDepartment());
        final Integer userGrade = user.getGrade();

        // 요약된 메세지(프롬프트) — FE/BE에서 이미 요약해서 넘어오는 값이라고 가정
        final String summary = nullToEmpty(request.getMessage());

        // 2) 선호 파싱
        final Integer maxDays = parseMaxDays(summary).orElse(null);       // "주 3일" 등
        final boolean avoidFirstPeriod = detectAvoidFirstPeriod(summary); // 1교시 피하기 여부

        // 3) 전체 과목
        List<Course> all = courseRepository.findAll();

        // 4) 재수강 후보(메시지 내 이름/코드 포함) — 학년·학과 제한 없이 우선 포함
        List<Course> retake = pickRetakeCourses(summary, all);

        // 5) 전공 후보 — 학과 ‘완전 일치’ + 추천학년 == 유저 학년
        List<Course> major = all.stream()
                .filter(this::isMajorCategory)  // 전공/전심/전선/전필 등
                .filter(c -> deptStrictMatch(userDept, normDept(c.getDepartment()))) // 학과 완전 일치
                .filter(c -> isMajorRecommendedGradeMatch(userGrade, c))             // 추천 학년 정확 일치
                .collect(Collectors.toList());

        // 6) 교양 후보(교필/교선)
        List<Course> liberal = all.stream()
                .filter(this::isLiberalCategory)
                .collect(Collectors.toList());

        // 7) 시간표 엔티티 생성
        Timetable timetable = Timetable.builder()
                .owner(user)
                .year(request.getYear())
                .semester(request.getSemester())
                .title(buildTitle(user, request))
                .items(new ArrayList<>()) // NPE 방지
                .build();
        timetableRepository.save(timetable);

        int totalCredits = 0;
        Set<String> usedCourseCodes = new HashSet<>();
        Map<String, List<int[]>> occupied = new HashMap<>(); // day -> [start,end)
        Set<String> usedDays = new HashSet<>();
        Set<String> usedCourseNames = new HashSet<>();       // 이름 중복 방지용

        CourseAdder adder = new CourseAdder(
                timetable,
                timetableItemRepository,
                usedCourseCodes,
                occupied,
                usedDays,
                usedCourseNames
        );

        // 정렬: 전공은 그냥 시간 빠른 순, 교양은 1교시 회피 옵션 반영
        major.sort(byStartPeriodWithAvoid(false));
        liberal.sort(byStartPeriodWithAvoid(avoidFirstPeriod));

        // 7-1) 재수강: 충돌/요일제한 무시하고 우선 포함(학점만 체크)
        totalCredits = adder.addCourses(
                retake,
                totalCredits,
                MAX_CREDITS,
                maxDays,
                false,  // applyFirstPeriodFilter
                true,   // forceAddRetake
                true    // ignoreDayLimit
        );

        // 현재까지 들어간 전공 개수(재수강에 전공 포함 가능)
        int majorCountSoFar = countMajorsInTimetable(timetable);

        // 7-2) 전공: 최대 5개까지만 추가
        int remainingMajorSlots = Math.max(0, MAX_MAJOR_COUNT - majorCountSoFar);
        if (remainingMajorSlots > 0) {
            List<Course> majorFiltered = major.stream()
                    .filter(c -> !usedCourseCodes.contains(nullToEmpty(c.getCourseCode())))
                    .collect(Collectors.toList());

            totalCredits = adder.addCoursesUpTo(
                    majorFiltered,
                    totalCredits,
                    MAX_CREDITS,
                    maxDays,
                    false,  // applyFirstPeriodFilter
                    false,  // forceAddRetake
                    false,  // ignoreDayLimit: 전공은 요일 제한 완화 안 함
                    remainingMajorSlots
            );
        }

        // 7-3) 교양 1차: 요일제한/1교시회피 반영
        List<Course> liberalFiltered = liberal.stream()
                .filter(c -> !usedCourseCodes.contains(nullToEmpty(c.getCourseCode())))
                .collect(Collectors.toList());

        totalCredits = adder.addCourses(
                liberalFiltered,
                totalCredits,
                MAX_CREDITS,
                maxDays,
                avoidFirstPeriod,
                false,
                false
        );

        // 7-4) 교양 2차: 아직 18학점 미만이면 요일 제한만 풀어서 한 번 더 시도
        if (totalCredits < MAX_CREDITS) {
            List<Course> liberalSecond = liberal.stream()
                    .filter(c -> !usedCourseCodes.contains(nullToEmpty(c.getCourseCode())))
                    .collect(Collectors.toList());

            totalCredits = adder.addCourses(
                    liberalSecond,
                    totalCredits,
                    MAX_CREDITS,
                    maxDays,
                    avoidFirstPeriod, // 1교시 회피는 유지
                    false,
                    true              // ignoreDayLimit
            );
        }

        // 8) AiTimetable 기록(유저당 1개)
        AiTimetable aiTimetable = aiTimetableRepository.findByUser_Id(user.getId())
                .orElseGet(() -> AiTimetable.builder()
                        .user(user)
                        .timetable(timetable)
                        .build()
                );

        // 요약된 메세지도 함께 저장 (summary -> prompt)
        aiTimetable.setPrompt(summary);

        // 시간표 결과 요약은 그대로 resultSummary에 저장
        aiTimetable.update(buildResultSummary(timetable), timetable);
        aiTimetableRepository.save(aiTimetable);

        return timetable;
    }

    /** 전공 과목 학년 매칭: recommended_grade가 유저 학년과 정확히 같은 과목만 허용 */
    private boolean isMajorRecommendedGradeMatch(Integer userGrade, Course c) {
        if (userGrade == null) return true;  // 유저 학년 정보 없으면 필터 안 함

        Integer rec = c.getRecommendedGrade();   // DB의 recommended_grade 컬럼 매핑

        // 추천학년이 비어 있으면 전공 후보에서 제외
        if (rec == null) return false;

        // 추천학년이 유저 학년이랑 딱 맞는 것만 허용
        return rec.equals(userGrade);
    }

    /** 전공 카테고리 여부 */
    private boolean isMajorCategory(Course c) {
        String cat = nullToEmpty(c.getCategory());
        return cat.startsWith("전") || cat.contains("전공") || cat.equals("전필") || cat.equals("전선")
                || cat.equalsIgnoreCase("major");
    }

    /** 교양 카테고리 여부(교필/교선) */
    private boolean isLiberalCategory(Course c) {
        String cat = nullToEmpty(c.getCategory());
        return cat.equals("교필") || cat.equals("교선");
    }

    /** timetable 내 전공 아이템 개수 */
    private int countMajorsInTimetable(Timetable t) {
        if (t.getItems() == null) return 0;
        int cnt = 0;
        for (TimetableItem it : t.getItems()) {
            String cat = nullToEmpty(it.getCategory());
            if (cat.startsWith("전") || cat.contains("전공") || cat.equals("전필") || cat.equals("전선")
                    || cat.equalsIgnoreCase("major")) cnt++;
        }
        return cnt;
    }

    /** 학과 엄격 매칭(정규화 후 완전 일치) */
    private boolean deptStrictMatch(String userDept, String courseDept) {
        if (userDept.isEmpty() || courseDept.isEmpty()) return false;
        return userDept.equals(courseDept);
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
                timetable.getItems().stream()
                        .mapToInt(i -> i.getCourse() != null ? i.getCourse().getCredit() : 0)
                        .sum();
        long majorCnt = timetable.getItems() == null ? 0 :
                timetable.getItems().stream().filter(i -> {
                    String cat = nullToEmpty(i.getCategory());
                    return cat.startsWith("전") || cat.contains("전공") || cat.equals("전필") || cat.equals("전선")
                            || cat.equalsIgnoreCase("major");
                }).count();
        long liberalCnt = timetable.getItems() == null ? 0 :
                timetable.getItems().stream().filter(i -> {
                    String cat = nullToEmpty(i.getCategory());
                    return cat.equals("교필") || cat.equals("교선");
                }).count();

        return String.format("전공 %d개(최대 %d), 교양 %d개, 총 %d학점",
                majorCnt, MAX_MAJOR_COUNT, liberalCnt, credits);
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

    /** 재수강 후보 탐색: 메시지에 이름/코드가 등장하면 픽업 */
    private static List<Course> pickRetakeCourses(String message, List<Course> all) {
        String msg = normalize(message);
        boolean cue = msg.contains("재수강") || msg.contains("재수")
                || msg.contains("retake") || msg.contains("repeat")
                || msg.contains("꼭들") || msg.contains("반드시") || msg.contains("다시들");
        if (!cue) return new ArrayList<>();

        List<Course> list = new ArrayList<>();
        for (Course c : all) {
            String name = normalize(c.getName());
            String code = normalize(c.getCourseCode());
            if ((!name.isEmpty() && containsToken(msg, name))
                    || (!code.isEmpty() && msg.contains(code))) {
                list.add(c);
            }
        }
        return list;
    }

    private static String normalize(String s) {
        s = nullToEmpty(s).toLowerCase().replaceAll("\\s+", "");
        return Normalizer.normalize(s, Normalizer.Form.NFC);
    }

    private static boolean containsToken(String hay, String needle) {
        if (needle.length() < 3) return false;
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
        Matcher m = Pattern.compile("주\\s*(\\d)\\s*일").matcher(nullToEmpty(msg));
        if (m.find()) {
            try {
                return Optional.of(Integer.parseInt(m.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return Optional.empty();
    }

    private static boolean detectAvoidFirstPeriod(String msg) {
        String m = nullToEmpty(msg).replaceAll("\\s+", "");
        return (m.contains("1교시") || m.contains("첫교시"))
                && (m.contains("피") || m.contains("빼") || m.contains("안") || m.contains("없"));
    }

    // =======================
    // 요약 조회 메서드
    // =======================

    /**
     * userId 기준으로 AI 시간표 생성에 사용된 요약 메세지(prompt)를 반환한다.
     * - AiTimetable.prompt 사용
     */
    @Transactional(readOnly = true)
    public TimetableSummaryResponse getTimetableSummary(Long userId) {
        AiTimetable entity = aiTimetableRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 AI 시간표가 없습니다."));

        String prompt = entity.getPrompt();
        if (prompt == null) {
            prompt = "";
        }

        return TimetableSummaryResponse.builder()
                .userId(userId)
                .summary(prompt)
                .build();
    }

    // =======================
    // AI 시간표 메타 저장/조회/삭제
    // =======================

    /** 수동 저장/수정 */
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

        // 여기서는 resultSummary만 수정 (prompt는 그대로 유지)
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
    // 과목 추가 헬퍼
    // =======================
    private static class CourseAdder {
        private final Timetable timetable;
        private final TimetableItemRepository repo;
        private final Set<String> usedCodes;
        private final Map<String, List<int[]>> occupied; // day -> [start,end)
        private final Set<String> usedDays;

        // 과목 이름(정규화) 중복 체크용
        private final Set<String> usedNames;

        private CourseAdder(Timetable timetable,
                            TimetableItemRepository repo,
                            Set<String> usedCodes,
                            Map<String, List<int[]>> occupied,
                            Set<String> usedDays,
                            Set<String> usedNames) {
            this.timetable = timetable;
            this.repo = repo;
            this.usedCodes = usedCodes;
            this.occupied = occupied;
            this.usedDays = usedDays;
            this.usedNames = usedNames;
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
                if (!canAdd(c, currentCredits, maxCredits, maxDays,
                        applyFirstPeriodFilter, forceAddRetake, ignoreDayLimit)) {
                    continue;
                }
                currentCredits = add(c, currentCredits);
            }
            return currentCredits;
        }

        /** 최대 N개까지만 추가(전공 제한용) */
        int addCoursesUpTo(List<Course> candidates,
                           int currentCredits,
                           int maxCredits,
                           Integer maxDays,
                           boolean applyFirstPeriodFilter,
                           boolean forceAddRetake,
                           boolean ignoreDayLimit,
                           int maxToAdd) {
            int added = 0;
            for (Course c : candidates) {
                if (currentCredits >= maxCredits) break;
                if (added >= maxToAdd) break;
                if (!canAdd(c, currentCredits, maxCredits, maxDays,
                        applyFirstPeriodFilter, forceAddRetake, ignoreDayLimit)) {
                    continue;
                }
                currentCredits = add(c, currentCredits);
                added++;
            }
            return currentCredits;
        }

        private boolean canAdd(Course c,
                               int currentCredits,
                               int maxCredits,
                               Integer maxDays,
                               boolean applyFirstPeriodFilter,
                               boolean forceAddRetake,
                               boolean ignoreDayLimit) {

            String code = nullToEmpty(c.getCourseCode());
            if (!code.isEmpty() && usedCodes.contains(code)) return false;

            // 이름 기준 중복 검사 (공백/대소문자 무시)
            String nameKey = normalize(c.getName());
            if (!nameKey.isEmpty() && usedNames.contains(nameKey)) {
                return false;
            }

            String day = nullToEmpty(c.getDayOfWeek());
            int start = safeInt(c.getStartPeriod());
            int endExclusive = safeInt(c.getEndPeriod()) + 1;

            if (applyFirstPeriodFilter && start == 1) return false;

            if (!ignoreDayLimit && maxDays != null) {
                boolean newDay = !usedDays.contains(day);
                if (newDay && (usedDays.size() + 1) > maxDays) return false;
            }

            boolean conflict = isConflict(day, start, endExclusive);
            if (conflict && !forceAddRetake) return false;

            int after = currentCredits + safeInt(c.getCredit());
            if (after > maxCredits) return false;

            return true;
        }

        private int add(Course c, int currentCredits) {
            TimetableItem item = TimetableItem.builder()
                    .timetable(timetable)
                    .course(c)
                    .courseName(c.getName())
                    .dayOfWeek(c.getDayOfWeek())
                    .startPeriod(c.getStartPeriod())
                    .endPeriod(c.getEndPeriod())
                    .room(c.getRoom())
                    .category(c.getCategory())
                    .build();
            repo.save(item);
            timetable.addItem(item);

            String code = nullToEmpty(c.getCourseCode());
            if (!code.isEmpty()) usedCodes.add(code);

            String nameKey = normalize(c.getName());
            if (!nameKey.isEmpty()) usedNames.add(nameKey);

            occupy(nullToEmpty(c.getDayOfWeek()),
                    safeInt(c.getStartPeriod()),
                    safeInt(c.getEndPeriod()) + 1);

            return currentCredits + safeInt(c.getCredit());
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
            occupied.computeIfAbsent(day, k -> new ArrayList<>())
                    .add(new int[]{start, endExclusive});
        }
    }
}