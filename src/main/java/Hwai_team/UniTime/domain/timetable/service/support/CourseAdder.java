package Hwai_team.UniTime.domain.timetable.service.support;

import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.timetable.entity.TimetableItem;
import Hwai_team.UniTime.domain.timetable.repository.TimetableItemRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 시간표에 강의를 채워 넣는 동안의 상태(중복/시간충돌/요일제한)를 관리하는 헬퍼.
 */
public class CourseAdder {

    private final Timetable timetable;
    private final TimetableItemRepository repo;
    private final Set<String> usedCodes = new HashSet<>();
    // dayOfWeek -> [startPeriod, endPeriodExclusive) 리스트
    private final Map<String, List<int[]>> occupied = new HashMap<>();
    private final Set<String> usedDays = new HashSet<>();
    private final Set<String> usedNames = new HashSet<>(); // 과목 이름(정규화) 중복 체크용

    public CourseAdder(Timetable timetable, TimetableItemRepository repo) {
        this.timetable = timetable;
        this.repo = repo;
    }

    public int addCourses(List<Course> candidates,
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

    public int addCoursesUpTo(List<Course> candidates,
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

        String nameKey = AiTimetableTextParser.normalize(c.getName());
        if (!nameKey.isEmpty() && usedNames.contains(nameKey)) return false;

        String day = nullToEmpty(c.getDayOfWeek());
        Integer startP = c.getStartPeriod();
        Integer endP = c.getEndPeriod();
        if (day.isEmpty() || startP == null || endP == null) {
            return false;
        }

        if (applyFirstPeriodFilter && startP == 1) return false;

        int start = safeInt(startP);
        int endExclusive = safeInt(endP) + 1;

        // 요일 제한
        if (!ignoreDayLimit && maxDays != null) {
            boolean newDay = !usedDays.contains(day);
            if (newDay && (usedDays.size() + 1) > maxDays) return false;
        }

        // 교시 구간만으로 겹침 체크
        if (isConflict(day, start, endExclusive)) return false;

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

        try {
            // 먼저 addItem으로 충돌 검사
            timetable.addItem(item);
        } catch (IllegalStateException e) {
            // 충돌났으면 아예 추가하지 않음 (DB에도 저장 안 됨)
            return currentCredits;
        }

        repo.save(item); // 검증 통과한 것만 저장

        String code = nullToEmpty(c.getCourseCode());
        if (!code.isEmpty()) usedCodes.add(code);

        String nameKey = AiTimetableTextParser.normalize(c.getName());
        if (!nameKey.isEmpty()) usedNames.add(nameKey);

        occupy(
                nullToEmpty(c.getDayOfWeek()),
                safeInt(c.getStartPeriod()),
                safeInt(c.getEndPeriod()) + 1
        );

        return currentCredits + safeInt(c.getCredit());
    }

    private boolean isConflict(String day, int start, int endExclusive) {
        if (day.isEmpty()) return false;
        List<int[]> list = occupied.getOrDefault(day, new ArrayList<>());
        for (int[] r : list) {
            int as = r[0], ae = r[1];
            if (start < ae && as < endExclusive) return true;
        }
        return false;
    }

    private void occupy(String day, int start, int endExclusive) {
        if (day.isEmpty()) return;
        usedDays.add(day);
        occupied.computeIfAbsent(day, k -> new ArrayList<>())
                .add(new int[]{start, endExclusive});
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static int safeInt(Integer i) {
        return i == null ? 0 : i;
    }
}
