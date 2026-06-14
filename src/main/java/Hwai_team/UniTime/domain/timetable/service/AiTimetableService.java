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
import Hwai_team.UniTime.domain.timetable.service.support.AiTimetableTextParser;
import Hwai_team.UniTime.domain.timetable.service.support.CourseAdder;
import Hwai_team.UniTime.domain.timetable.service.support.CourseCategory;
import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static Hwai_team.UniTime.domain.timetable.service.support.AiTimetableTextParser.normDept;
import static Hwai_team.UniTime.domain.timetable.service.support.AiTimetableTextParser.nullToEmpty;
import static Hwai_team.UniTime.domain.timetable.service.support.AiTimetableTextParser.safeInt;

@Setter
@Service
@RequiredArgsConstructor
public class AiTimetableService {

    private static final int MAX_CREDITS = 19;                 // 시간표 최대 학점
    private static final int MAX_MAJOR_COUNT = 4;              // 최대 전공 수

    // 같은 프롬프트라도 매번 다른 시간표가 나오도록 후보 순서를 섞는 데 사용
    private static final Random RANDOM = new Random();

    private final AiTimetableRepository aiTimetableRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TimetableRepository timetableRepository;
    private final TimetableItemRepository timetableItemRepository;

    /**
     * <AI 시간표 생성 서비스>
     * AI가 사용자의 자연어를 토대로 시간표를 생성합니다.
     *
     * @author 김민호
     * @param request AI 시간표 생성 요청
     * @return 생성된 시간표
     */
    @Transactional
    public Timetable createByAi(AiTimetableRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + request.getUserId()));

        final String userDeptNorm = normDept(user.getDepartment());       // 사용자 학과
        final Integer userGrade = user.getGrade();                        // 사용자 학년
        final String planKey = AiTimetableTextParser.normalizePlanKey(request.getPlanKey());

        // 사용자가 작성한 자연어 메세지
        final String rawMessage = request.getMessage();
        final String summary = nullToEmpty(rawMessage);              // 파싱용 문자열

        // 사용자의 원하는 조건
        final Integer maxDays = AiTimetableTextParser.parseMaxDays(summary).orElse(null);  // "주 3일" 등
        final boolean avoidFirstPeriod = AiTimetableTextParser.detectAvoidFirstPeriod(summary); // 1교시 회피 여부
        final Set<String> offDays = AiTimetableTextParser.parseOffDays(summary);                // "금요일 공강" 등 공강 요일

        // 전체 과목 리스트 불러오기
        List<Course> all = courseRepository.findAll();

        // 채팅 안에서 타겟 학과/학년 추론
        final String targetDeptNorm = AiTimetableTextParser.resolveTargetDepartment(summary, userDeptNorm, all);
        final Integer targetGrade = AiTimetableTextParser.resolveTargetGrade(summary, userGrade);

        List<Course> retake = buildRetakeCandidates(summary, all, offDays);
        List<Course> major = buildMajorCandidates(all, targetDeptNorm, targetGrade, offDays);
        List<Course> liberal = buildLiberalCandidates(all, offDays);

        Timetable timetable = Timetable.builder()
                .owner(user)
                .year(request.getYear())
                .semester(request.getSemester())
                .title(buildTitle(user, request))
                .items(new ArrayList<>())
                .build();
        timetableRepository.save(timetable);

        fillTimetable(timetable, retake, major, liberal, maxDays, avoidFirstPeriod);

        // MAX_CREDITS 초과 시 뒤에서 과목 잘라내기
        enforceMaxCredits(timetable);

        upsertAiTimetable(user, planKey, rawMessage, summary, timetable);

        return timetable;
    }

    /** 우선순위(재수강 > 전공 > 교양)에 따라 시간표에 강의를 채워 넣는다. */
    private void fillTimetable(Timetable timetable,
                                List<Course> retake,
                                List<Course> major,
                                List<Course> liberal,
                                Integer maxDays,
                                boolean avoidFirstPeriod) {

        CourseAdder adder = new CourseAdder(timetable, timetableItemRepository);

        // 후보 순서를 무작위로 섞어, 같은 프롬프트라도 매번 다른 시간표가 나오도록 함
        Collections.shuffle(retake, RANDOM);
        Collections.shuffle(major, RANDOM);
        Collections.shuffle(liberal, RANDOM);

        // 교양은 1교시 회피 옵션만 반영 (그 외 순서는 위에서 섞인 순서 유지)
        liberal.sort(byAvoidFirstPeriod(avoidFirstPeriod));

        // 우선순위1: 재수강 (요일 제한도 지킴)
        int totalCredits = adder.addCourses(
                retake,
                0,
                MAX_CREDITS,
                maxDays,
                false,  // applyFirstPeriodFilter
                false,  // forceAddRetake (현재 사용 안 함)
                false   // ignoreDayLimit
        );

        // 2순위: 전공 (최대 4개까지만 추가, 요일 제한 지킴)
        int majorCountSoFar = countMajorsInTimetable(timetable);
        int remainingMajorSlots = Math.max(0, MAX_MAJOR_COUNT - majorCountSoFar);
        if (remainingMajorSlots > 0) {
            totalCredits = adder.addCoursesUpTo(
                    major,
                    totalCredits,
                    MAX_CREDITS,
                    maxDays,
                    false,
                    false,
                    false,
                    remainingMajorSlots
            );
        }

        // 3순위: 교양 (1차: 요일제한/1교시회피 반영)
        totalCredits = adder.addCourses(
                liberal,
                totalCredits,
                MAX_CREDITS,
                maxDays,
                avoidFirstPeriod,
                false,
                false
        );

        // 교양 2차: 아직 19학점 미만이고, "주 X일" 제한이 없을 때만 요일 제한 풀어서 시도
        if (totalCredits < MAX_CREDITS && maxDays == null) {
            adder.addCourses(
                    liberal,
                    totalCredits,
                    MAX_CREDITS,
                    null,             // maxDays 없음
                    avoidFirstPeriod, // 1교시 회피는 유지
                    false,
                    true              // ignoreDayLimit
            );
        }
    }

    /** 재수강 후보: 메시지에서 언급된 과목, 공강 요일 제외 */
    private List<Course> buildRetakeCandidates(String summary, List<Course> all, Set<String> offDays) {
        return AiTimetableTextParser.pickRetakeCourses(summary, all).stream()
                .filter(c -> !offDays.contains(nullToEmpty(c.getDayOfWeek())))
                .collect(Collectors.toList());
    }

    /** 전공 후보: 타겟 학과/학년 일치, 공강 요일 제외 */
    private List<Course> buildMajorCandidates(List<Course> all, String targetDeptNorm, Integer targetGrade, Set<String> offDays) {
        return all.stream()
                .filter(c -> CourseCategory.isMajor(c.getCategory()))
                .filter(c -> deptStrictMatch(targetDeptNorm, normDept(c.getDepartment())))
                .filter(c -> isMajorRecommendedGradeMatch(targetGrade, c))
                .filter(c -> !offDays.contains(nullToEmpty(c.getDayOfWeek())))
                .collect(Collectors.toList());
    }

    /** 교양 후보: 공강 요일 제외 */
    private List<Course> buildLiberalCandidates(List<Course> all, Set<String> offDays) {
        return all.stream()
                .filter(c -> CourseCategory.isLiberal(c.getCategory()))
                .filter(c -> !offDays.contains(nullToEmpty(c.getDayOfWeek())))
                .collect(Collectors.toList());
    }

    /** AiTimetable 기록 upsert (유저당 플랜별 1개) */
    private void upsertAiTimetable(User user, String planKey, String rawMessage, String summary, Timetable timetable) {
        AiTimetable aiTimetable = aiTimetableRepository
                .findByUser_IdAndPlanKey(user.getId(), planKey)
                .orElseGet(() -> AiTimetable.builder()
                        .user(user)
                        .timetable(timetable)
                        .planKey(planKey)
                        .build()
                );

        aiTimetable.setMessage(rawMessage);                     // 사용자가 남긴 자연어 원본
        aiTimetable.setPrompt(summary);                         // 파싱/요약용 문자열
        aiTimetable.update(buildResultSummary(timetable), timetable); // 결과 요약
        aiTimetableRepository.save(aiTimetable);
    }

    /**
     * <사용자 학년에 맞는 강의 매칭 필터>
     * 사용자의 학년과 강의의 recommended_grade가 같은지 대조합니다.
     *
     * @author 김민호
     * @param targetGrade 타겟 학년
     * @param c 대상 강의
     * @return 일치 여부
     */
    private boolean isMajorRecommendedGradeMatch(Integer targetGrade, Course c) {
        if (targetGrade == null) return true;  // 타겟 학년 정보 없으면 필터 안 함

        Integer rec = c.getRecommendedGrade(); // DB의 recommended_grade 컬럼 매핑
        if (rec == null) return false;         // 추천 학년이 비어 있으면 전공 후보에서 제외

        return rec.equals(targetGrade);
    }

    /**
     * <시간표 내 전공 갯수 카운터>
     * 시간표 내 전공이 몇개인지 셉니다
     *
     * @author 김민호
     * @param t 대상 시간표
     * @return 전공 강의 개수
     */
    private int countMajorsInTimetable(Timetable t) {
        if (t.getItems() == null) return 0;
        int cnt = 0;
        for (TimetableItem it : t.getItems()) {
            if (CourseCategory.isMajor(it.getCategory())) cnt++;
        }
        return cnt;
    }

    /**
     * 학과 엄격 매칭 헬퍼 함수
     * 강의의 학과와 유저의 학과가 일치하는지 엄격하게 확인합니다.
     *
     * @author 김민호
     * @param userDept 정규화된 유저 학과
     * @param courseDept 정규화된 강의 학과
     * @return 일치 여부
     */
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

    /**
     * <시간표 내 강의 학점 총합이 19학점이 안넘도록 도와주는 함수>
     * 시간표 내 학점 총합이 19학점 안넘도록
     *
     * @author 김민호
     * @param timetable 대상 시간표
     */
    private void enforceMaxCredits(Timetable timetable) {
        if (timetable.getItems() == null || timetable.getItems().isEmpty()) {
            return;
        }

        List<TimetableItem> items = new ArrayList<>(timetable.getItems());

        int sum = 0;
        for (TimetableItem it : items) {
            if (it.getCourse() != null && it.getCourse().getCredit() != null) {
                sum += it.getCourse().getCredit();
            }
        }

        if (sum <= MAX_CREDITS) {
            return;
        }

        // 뒤에서부터 하나씩 삭제 (보통 나중에 들어간 교양 과목부터 빠짐)
        ListIterator<TimetableItem> it = items.listIterator(items.size());
        while (sum > MAX_CREDITS && it.hasPrevious()) {
            TimetableItem last = it.previous();

            Integer credit = (last.getCourse() != null) ? last.getCourse().getCredit() : null;
            int c = (credit != null) ? credit : 0;
            sum -= c;

            // DB/엔티티 둘 다에서 제거
            timetableItemRepository.delete(last);
            timetable.getItems().remove(last);
        }
    }

    /**
     * <결과 요약 함수>
     * 결과 요약을 반환해용
     *
     * @author 김민호
     * @param timetable 대상 시간표
     * @return 요약 문자열 (전공/교양 개수, 총 학점, 등교 요일 등)
     */
    private static String buildResultSummary(Timetable timetable) {
        if (timetable.getItems() == null || timetable.getItems().isEmpty()) {
            return "전공 0개(최대 " + MAX_MAJOR_COUNT + "), 교양 0개, 총 0학점, 주 0일";
        }

        int credits = timetable.getItems().stream()
                .mapToInt(i -> i.getCourse() != null ? i.getCourse().getCredit() : 0)
                .sum();

        long majorCnt = timetable.getItems().stream()
                .filter(i -> CourseCategory.isMajor(i.getCategory()))
                .count();

        long liberalCnt = timetable.getItems().stream()
                .filter(i -> CourseCategory.isLiberal(i.getCategory()))
                .count();

        // 사용된 요일 수집
        Set<String> daySet = new HashSet<>();
        for (TimetableItem it : timetable.getItems()) {
            daySet.add(nullToEmpty(it.getDayOfWeek()));
        }

        int dayCount = (int) daySet.stream()
                .filter(d -> !d.isBlank())
                .count();

        List<String> orderedDays = Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT");
        List<String> usedDaysKorean = orderedDays.stream()
                .filter(daySet::contains)
                .map(AiTimetableService::toKoreanDay)
                .collect(Collectors.toList());

        String dayPart = usedDaysKorean.isEmpty()
                ? "주 0일"
                : "주 " + dayCount + "일 (" + String.join(", ", usedDaysKorean) + ")";

        return String.format("전공 %d개(최대 %d), 교양 %d개, 총 %d학점, %s",
                majorCnt, MAX_MAJOR_COUNT, liberalCnt, credits, dayPart);
    }

    /**
     * <요일 한국어 변환>
     * 디비에 영어로 된 요일을 한국어로 변환 해줍니다.
     *
     * @author 김민호
     * @param day 영문 요일 코드 (MON/TUE/...)
     * @return 한글 요일
     */
    private static String toKoreanDay(String day) {
        switch (day) {
            case "MON": return "월";
            case "TUE": return "화";
            case "WED": return "수";
            case "THU": return "목";
            case "FRI": return "금";
            case "SAT": return "토";
            case "SUN": return "일";
            default:    return day;
        }
    }

    /**
     * 1교시 회피 옵션이 켜져 있으면 1교시 강의를 뒤로 밀어내고,
     * 그 외에는 순서를 그대로 유지한다 (셔플된 순서를 보존하기 위해 안정 정렬용으로 사용).
     */
    private static Comparator<Course> byAvoidFirstPeriod(boolean avoidFirstPeriod) {
        return (a, b) -> {
            if (!avoidFirstPeriod) return 0;
            int sa = safeInt(a.getStartPeriod());
            int sb = safeInt(b.getStartPeriod());
            if (sa == 1 && sb != 1) return 1;
            if (sb == 1 && sa != 1) return -1;
            return 0;
        };
    }

    /**
     * userId 기준으로 AI 시간표 생성에 사용된 요약 메세지(prompt)를 반환
     * - 여러 플랜 중 createdAt 최신 1개 기준
     */
    @Transactional(readOnly = true)
    public TimetableSummaryResponse getTimetableSummary(Long userId) {
        List<AiTimetable> list = aiTimetableRepository.findAllByUser_Id(userId);

        if (list.isEmpty()) {
            return TimetableSummaryResponse.builder()
                    .userId(userId)
                    .summary("")
                    .build();
        }

        AiTimetable latest = list.stream()
                .filter(it -> it.getCreatedAt() != null)
                .max(Comparator.comparing(AiTimetable::getCreatedAt))
                .orElse(list.get(0));

        String prompt = latest.getPrompt();
        if (prompt == null) prompt = "";

        return TimetableSummaryResponse.builder()
                .userId(userId)
                .summary(prompt)
                .build();
    }

    // =======================
    // AI 시간표 메타 저장/조회/삭제
    // =======================

    /** 수동 저장/수정 (플랜별) */
    @Transactional
    public AiTimetableResponse saveAiTimetable(AiTimetableSaveRequest request) {
        if (request.getUserId() == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (request.getTimetableId() == null) throw new IllegalArgumentException("timetableId는 필수입니다.");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        Timetable timetable = timetableRepository.findById(request.getTimetableId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시간표입니다."));

        String planKey = AiTimetableTextParser.normalizePlanKey(request.getPlanKey());

        AiTimetable aiTimetable = aiTimetableRepository
                .findByUser_IdAndPlanKey(user.getId(), planKey)
                .orElseGet(() -> AiTimetable.builder()
                        .user(user)
                        .timetable(timetable)
                        .planKey(planKey)
                        .build()
                );

        // 여기서는 resultSummary만 수정 (prompt는 그대로 유지)
        aiTimetable.update(request.getResultSummary(), timetable);
        return AiTimetableResponse.from(aiTimetableRepository.save(aiTimetable));
    }

    /**
     * <AI 시간표 조회 서비스(Plan A,B,C)>
     * AI가 만든 시간표를 조회 합니다.
     *
     * @author 김민호
     * @param userId 유저 id
     * @param planKey 플랜 키 (A/B/C)
     * @return AI 시간표 응답
     */
    @Transactional(readOnly = true)
    public AiTimetableResponse getAiTimetable(Long userId, String planKey) {
        String pk = AiTimetableTextParser.normalizePlanKey(planKey);
        AiTimetable entity = aiTimetableRepository.findByUser_IdAndPlanKey(userId, pk)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 AI 시간표가 없습니다. plan=" + pk));
        return AiTimetableResponse.from(entity);
    }

    /**
     * <AI 시간표 조회 서비스>
     * AI가 만든 시간표를 조회 합니다.
     *
     * @author 김민호
     * @param userId 유저 id
     * @return AI 시간표 응답 목록
     */
    @Transactional(readOnly = true)
    public List<AiTimetableResponse> getAiTimetables(Long userId) {
        List<AiTimetable> list = aiTimetableRepository.findAllByUser_Id(userId);
        return list.stream()
                .map(AiTimetableResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * <AI 시간표 삭제 서비스>
     * AI가 만든 시간표를 삭제 합니다.
     *
     * @author 김민호
     * @param userId 유저 id
     * @param planKey 플랜 키 (A/B/C, null이면 전체 삭제)
     */
    @Transactional
    public void deleteAiTimetable(Long userId, String planKey) {
        if (planKey == null || planKey.isBlank()) {
            aiTimetableRepository.deleteByUser_Id(userId);
        } else {
            String pk = AiTimetableTextParser.normalizePlanKey(planKey);
            aiTimetableRepository.deleteByUser_IdAndPlanKey(userId, pk);
        }
    }
}
