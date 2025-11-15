package Hwai_team.UniTime.global.ai;

import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.user.entity.User;

import java.util.List;

public class PromptTemplates {
    public static final String CHAT_SYSTEM_PROMPT = """
        너는 UniTime 서비스 안에서 동작하는 대학생 전용 챗봇이야.
        시간표, 수강 과목, 공부 계획, 그리고 UniTime 기능 사용법 등에 대해
        간결하고 정확하게 한국어로 답변해줘.
        """;

    public static String buildTimetablePrompt(User user, List<Course> courses, String userMessage) {
        StringBuilder sb = new StringBuilder();

        // 1️⃣ 기본 정보
        sb.append("""
        너는 대학교 시간표 생성 AI야.
        아래 제공된 과목 목록(JSON 배열)만을 사용해서 시간표를 만들어야 해.
        새로운 과목을 추가하거나 이름을 바꾸면 안 된다.
        반드시 JSON만 반환해야 한다.

        [학생 정보]
        """);
        sb.append("이름: ").append(user.getName()).append("\n");
        sb.append("학과: ").append(user.getDepartment()).append("\n");
        sb.append("학년: ").append(user.getGrade()).append("\n");
        sb.append("요청 내용: ").append(userMessage).append("\n\n");

        // 2️⃣ 과목 목록 JSON 배열로 변환
        sb.append("[\n");
        for (int i = 0; i < courses.size(); i++) {
            Course c = courses.get(i);
            sb.append("  {\n")
                    .append("    \"courseCode\": \"").append(c.getCourseCode()).append("\",\n")
                    .append("    \"name\": \"").append(c.getName()).append("\",\n")
                    .append("    \"credit\": ").append(c.getCredit()).append(",\n")
                    .append("    \"category\": \"").append(c.getCategory()).append("\",\n")
                    .append("    \"department\": \"").append(c.getDepartment()).append("\",\n")
                    .append("    \"recommendedGrade\": ").append(c.getRecommendedGrade()).append(",\n")
                    .append("    \"dayOfWeek\": \"").append(c.getDayOfWeek()).append("\",\n")
                    .append("    \"startPeriod\": ").append(c.getStartPeriod()).append(",\n")
                    .append("    \"endPeriod\": ").append(c.getEndPeriod()).append(",\n")
                    .append("    \"professor\": \"").append(c.getProfessor() != null ? c.getProfessor() : "").append("\",\n")
                    .append("    \"room\": \"").append(c.getRoom() != null ? c.getRoom() : "").append("\"\n")
                    .append("  }");
            if (i < courses.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n\n");

        // 3️⃣ 강제 규칙
        sb.append("""
        ⚠️ 절대규칙:
        - 반드시 위 JSON 배열에 있는 과목만 선택해야 한다.
        - 새로운 과목을 생성하거나 이름을 바꾸지 마라.
        - courseCode, name, dayOfWeek, startPeriod, endPeriod, room, professor 값은 그대로 사용해야 한다.
        - 과목명(name)은 반드시 위 JSON에 있는 한국어 그대로 써라.
        - 영어 이름, 약칭, 번역된 이름은 절대 쓰지 마라.
        - 총 학점은 19을 넘지 않는다.
        - 동일한 요일, 교시가 겹치는 과목은 동시에 선택하지 않는다.
        - 학과/학년에 맞는 전공 과목만 포함하고, 남는 학점은 교양(교필/교선)으로 채워라.
        - “재수강”, “주 X일만 학교”, “1교시 피하기” 등의 요청은 반드시 반영하라.

        [출력 형식]
        반드시 아래 JSON 형식만 출력하라. 다른 문장, 설명, 이유 등은 절대 쓰지 마라.

        {
          "title": "string",
          "items": [
            {
              "courseCode": "string",   // 반드시 위 목록 중 하나
              "courseName": "string",   // 위 목록의 name 그대로
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
}