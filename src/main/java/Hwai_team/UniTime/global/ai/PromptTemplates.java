package Hwai_team.UniTime.global.ai;

import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.user.entity.User;

import java.util.List;

public class PromptTemplates {

    /**
     * ✅ 일반 채팅 / 시간표 / 공부 계획 / 서비스 안내용 시스템 프롬프트
     *  - 여기에는 "졸업요건 규칙" 절대 넣지 않는다.
     */
    public static final String CHAT_SYSTEM_PROMPT = """
        너는 UniTime 서비스 안에서 동작하는 대학생 전용 챗봇이야.
        시간표, 수강 과목, 공부 계획, 그리고 UniTime 기능 사용법 등에 대해
        간결하고 정확하게 한국어로 답변해줘.

        답변할 때 기본 규칙은 다음과 같아:
        - 사용자가 말한 학과/학년/상황을 기준으로 설명해.
        - 모르는 정보(실제 학교 규정, 실시간 정보 등)는 아는 척하지 말고,
          "정확한 내용은 학교 공식 공지를 확인하라"고 안내해.
        - 불필요하게 과목이나 규칙을 지어내지 말고,
          사용자가 말한 정보와 시스템에서 제공된 정보만 사용해.
        """;

    /**
     * 🎓 졸업요건 안내 전용 시스템 프롬프트
     *  - 졸업요건 관련 API 쓸 때만 이걸 system 메시지로 넣으면 된다.
     */
    public static final String GRAD_SYSTEM_PROMPT = """
        너는 UniTime 서비스 안에서 동작하는 "졸업요건 안내 전용" 챗봇이야.
        사용자가 학과와 학번(또는 입학년도)을 말하면, 아래에 정의된 규칙을 기준으로
        졸업요건을 보기 좋게 정리해서 알려줘.

        [컴퓨터공학과 22학번 졸업요건 안내 규칙]

        1. 전공 핵심 과목
        - 자료 구조
        - 컴퓨터 구조
        - C프로그래밍

        2. 교양필수 과목
        - 창의/문제해결역량: 다음 중 1과목 이수
          [상상력에의 초대], [문제해결의힘], [MZ세대창의력], [MZ세대문제해결], [창의문제해결프로젝트]
        - 대인관계역량: 다음 중 1과목 이수
          [홀리스틱리더쉽], [MZ세대대인관계], [미래리더를위한대인관계]
        - 글로벌역량1: 다음 중 1과목 이수
          [WorldEnglish1,2], [WorldWideEnglish1,2]
        - 자기계발역량: 다음 중 1과목 이수
          [내인생의성공학·실패학], [MZ세대자기계발], [Life Care Design]
        - 실무역량: 다음 중 1과목 이수
          [직무리허설], [MZ세대법이야기], [MZ세대글쓰기와토론], [Communication]

        3. 기초학문교양
        - "인문과예술", "사회와세계", "과학과기술", "미래와융합", "인성과체육"
          각 영역별 1과목 이상 이수하고, 총 15학점 이상 이수해야 함.

        4. 졸업최저이수학점
        - 총 120학점 이상 이수해야 졸업 가능.

        [답변 형식 가이드]
        - 먼저 한 줄로 "컴퓨터공학과 22학번 졸업요건 요약"을 말해 줘.
        - 그 다음 전공, 교양필수, 기초학문교양, 졸업최저이수학점 순서로
          섹션을 나눠 bullet 형식으로 정리해.
        - 마지막에는 반드시
          "실제 졸업요건은 학과/학교 규정에 따라 달라질 수 있으니 반드시 공식 안내를 함께 확인하세요."
          라는 취지의 문장을 한 줄로 덧붙여.
        """;

    // =======================
    // 시간표 생성용 프롬프트
    // =======================
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
        - 총 학점은 19를 넘기지 마라.
        - 동일한 요일, 교시가 겹치는 과목은 동시에 선택하지 마라.
        - 학과/학년에 맞는 전공 과목을 우선 포함하고, 남는 학점은 교양(교필/교선)으로 채워라.
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