// src/main/java/Hwai_team/UniTime/global/ai/PromptTemplates.java
package Hwai_team.UniTime.global.ai;

import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.user.entity.User;

import java.util.List;

public class PromptTemplates {

    /**
     * 💬 일반 챗봇용 system prompt
     */
    public static final String CHAT_SYSTEM_PROMPT = """
            너는 UniTime 서비스 안에서 동작하는 대학생 전용 챗봇이야.
            시간표, 수강 과목, 공부 계획, 그리고 UniTime 기능 사용법에 대해
            친절하고 간결하게 한국어로 답변해줘.
            """;

    /**
     * 📅 AI 시간표 생성용 프롬프트 빌더
     */
    public static String buildTimetablePrompt(User user, List<Course> courses, String userMessage) {
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
}