package Hwai_team.UniTime.domain.timetable.service.support;

/**
 * 강의/시간표 항목의 이수구분(전공/교양 등)을 판별하는 헬퍼.
 */
public final class CourseCategory {

    private CourseCategory() {
    }

    /** 전공/전필/전선 등 전공 계열 카테고리인지 판별 */
    public static boolean isMajor(String category) {
        String cat = category == null ? "" : category;
        return cat.startsWith("전") || cat.contains("전공") || cat.equals("전필") || cat.equals("전선")
                || cat.equalsIgnoreCase("major");
    }

    /** 교필/교선 등 교양 계열 카테고리인지 판별 */
    public static boolean isLiberal(String category) {
        String cat = category == null ? "" : category;
        return cat.equals("교필") || cat.equals("교선");
    }
}
