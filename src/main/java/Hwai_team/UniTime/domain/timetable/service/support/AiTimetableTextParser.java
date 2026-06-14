package Hwai_team.UniTime.domain.timetable.service.support;

import Hwai_team.UniTime.domain.course.entity.Course;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 시간표 생성 요청의 자연어 메시지에서 조건(요일/학년/학과/재수강 등)을 추출하는 파서.
 */
public final class AiTimetableTextParser {

    private AiTimetableTextParser() {
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static int safeInt(Integer i) {
        return i == null ? 0 : i;
    }

    /** 공백 제거 + NFC 정규화 + 소문자 변환 */
    public static String normalize(String s) {
        if (s == null) return "";
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        return s.toLowerCase().replaceAll("\\s+", "");
    }

    /** 학과명 비교용 정규화 (공백 제거 + NFC + 소문자) */
    public static String normDept(String s) {
        s = nullToEmpty(s);
        s = s.replaceAll("\\s+", "");
        return Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase();
    }

    public static String normalizePlanKey(String planKey) {
        if (planKey == null || planKey.isBlank()) return "A";
        String upper = planKey.trim().toUpperCase();
        if (!upper.equals("A") && !upper.equals("B") && !upper.equals("C")) return "A";
        return upper;
    }

    /**
     * <사용자가 원하는 공강 추출 함수>
     * "금요일 공강", "수요일 빼고", "월요일은 쉬고 싶어요" 같은 표현에서 공강 요일 추출
     *
     * @author 김민호
     * @param msg 자연어 메시지
     * @return 공강 요일 코드(MON/TUE/...) 집합
     */
    public static Set<String> parseOffDays(String msg) {
        String text = nullToEmpty(msg).replaceAll("\\s+", "");

        Set<String> result = new HashSet<>();

        Map<String, String> dayMap = new HashMap<>();
        dayMap.put("월", "MON");
        dayMap.put("화", "TUE");
        dayMap.put("수", "WED");
        dayMap.put("목", "THU");
        dayMap.put("금", "FRI");
        dayMap.put("토", "SAT");
        dayMap.put("일", "SUN");

        Pattern p = Pattern.compile("(월|화|수|목|금|토|일)요일?(공강|빼고|제외|쉬고싶|쉬고싶어요|수업없|없었으면)");
        Matcher m = p.matcher(text);

        while (m.find()) {
            String kor = m.group(1);
            String code = dayMap.get(kor);
            if (code != null) {
                result.add(code);
            }
        }

        return result;
    }

    /**
     * "주 3일", "주 3회", "일주일에 3번", "3일만 학교" 등 여러 표현에서 주당 등교 희망 일수를 추출
     */
    public static Optional<Integer> parseMaxDays(String msg) {
        String text = nullToEmpty(msg);

        // 1) "주 3일"
        Matcher m1 = Pattern.compile("주\\s*(\\d)\\s*일").matcher(text);
        if (m1.find()) {
            try {
                return Optional.of(Integer.parseInt(m1.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }

        // 2) "주 3회", "주 3번"
        Matcher m2 = Pattern.compile("주\\s*(\\d)\\s*(회|번)").matcher(text);
        if (m2.find()) {
            try {
                return Optional.of(Integer.parseInt(m2.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }

        // 3) "일주일에 3번", "일주일에 3일"
        Matcher m3 = Pattern.compile("일주일에\\s*(\\d)\\s*(번|일|회)").matcher(text);
        if (m3.find()) {
            try {
                return Optional.of(Integer.parseInt(m3.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }

        // 4) "3일만 학교", "3번만 학교"
        Matcher m4 = Pattern.compile("(\\d)\\s*(일|번|회)만\\s*학교").matcher(text);
        if (m4.find()) {
            try {
                return Optional.of(Integer.parseInt(m4.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }

        return Optional.empty();
    }

    /** "1교시 피하고 싶어요" 같은 1교시 회피 표현 감지 */
    public static boolean detectAvoidFirstPeriod(String msg) {
        String m = nullToEmpty(msg).replaceAll("\\s+", "");
        return (m.contains("1교시") || m.contains("첫교시"))
                && (m.contains("피") || m.contains("빼") || m.contains("안") || m.contains("없"));
    }

    /**
     * <채팅에서 학년 선정 서비스>
     * 채팅 내용에서 "3학년", "4학년" 같은 표현을 찾아 타겟 학년 반환.
     * 없으면 userGrade 그대로 씀.
     *
     * @author 김민호
     * @param summary 자연어 메시지
     * @param userGrade 사용자 학년
     * @return 타겟 학년
     */
    public static Integer resolveTargetGrade(String summary, Integer userGrade) {
        String text = nullToEmpty(summary).replaceAll("\\s+", "");
        Matcher m = Pattern.compile("(\\d)학년").matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return userGrade;
    }

    /**
     * <시간표의 학과 선정 함수>
     * - 메시지에 학과명이 없으면 userDeptNorm 그대로 반환
     * - 메시지에 여러 학과가 있으면, 첫 번째로 매칭된 학과를 사용
     *
     * @author 김민호
     * @param summary 자연어 메시지
     * @param userDeptNorm 정규화된 사용자 학과
     * @param all 전체 강의 목록
     * @return 정규화된 타겟 학과
     */
    public static String resolveTargetDepartment(String summary, String userDeptNorm, List<Course> all) {
        String msgNorm = normDept(summary);
        if (msgNorm.isEmpty()) {
            return userDeptNorm;
        }

        String chosen = userDeptNorm;
        for (Course c : all) {
            String originalDept = nullToEmpty(c.getDepartment());
            if (originalDept.isEmpty()) continue;

            String courseDeptNorm = normDept(originalDept);
            if (courseDeptNorm.isEmpty()) continue;

            // 유저 학과와 동일하면 override 할 필요 없음
            if (courseDeptNorm.equals(userDeptNorm)) continue;

            // 메시지에 다른 학과명이 포함되어 있으면 그 학과를 사용
            if (msgNorm.contains(courseDeptNorm)) {
                chosen = courseDeptNorm;
                break; // 첫 매칭 학과 사용
            }
        }

        return chosen;
    }

    /** 재수강 후보 탐색: 메시지에 이름/코드가 등장하면 픽업 */
    public static List<Course> pickRetakeCourses(String message, List<Course> all) {
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

    private static boolean containsToken(String hay, String needle) {
        if (needle.length() < 3) return false;
        return hay.contains(needle);
    }
}
