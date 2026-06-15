// src/main/java/Hwai_team/UniTime/domain/timetable/service/TimetableImageImportService.java
package Hwai_team.UniTime.domain.timetable.service;

import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.course.repository.CourseRepository;
import Hwai_team.UniTime.domain.timetable.dto.TimetableImageImportResponse;
import Hwai_team.UniTime.global.ai.OpenAiClient;
import Hwai_team.UniTime.global.ai.PromptTemplates;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimetableImageImportService {

    private final ObjectMapper objectMapper;
    private final CourseRepository courseRepository;
    private final OpenAiClient openAiClient;

    public TimetableImageImportResponse importFromImage(MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                throw new IllegalArgumentException("업로드된 이미지가 비어 있습니다.");
            }

            // 1) 이미지 base64 인코딩
            String base64 = Base64.getEncoder().encodeToString(image.getBytes());

            // 2) OpenAI Vision 호출 → JSON 문자열
            String json = openAiClient.askVisionJson(
                    PromptTemplates.TIMETABLE_IMAGE_IMPORT_SYSTEM_PROMPT,
                    PromptTemplates.TIMETABLE_IMAGE_IMPORT_USER_PROMPT,
                    base64
            );

            // 3) JSON → OCR 결과 리스트(raw)
            List<TimetableImageImportResponse.Item> rawItems =
                    objectMapper.readValue(
                            json,
                            new TypeReference<List<TimetableImageImportResponse.Item>>() {}
                    );

            // 4) 각 항목을 Course와 매핑 (없으면 임시 Course 생성)
            List<TimetableImageImportResponse.Item> mappedItems = new ArrayList<>();
            for (TimetableImageImportResponse.Item raw : rawItems) {
                Course course = resolveOrCreateCourse(raw);

                // 응답에 courseId, courseCode, category, credit까지 포함시키고 싶다면
                // Item에 해당 필드들이 있어야 함.
                TimetableImageImportResponse.Item item = TimetableImageImportResponse.Item.builder()
                        .courseId(course.getId())
                        .courseName(course.getName())
                        .courseCode(course.getCourseCode())
                        .dayOfWeek(raw.getDayOfWeek())
                        .startPeriod(raw.getStartPeriod())
                        .endPeriod(raw.getEndPeriod())
                        .room(raw.getRoom())
                        .category(course.getCategory())
                        .credit(course.getCredit())
                        .build();

                mappedItems.add(item);
            }

            return TimetableImageImportResponse.builder()
                    .items(mappedItems)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("시간표 이미지 분석에 실패했습니다.", e);
        }
    }

    /**
     * OCR로 나온 한 과목 정보를 가지고
     * 1) 기존 Course 매칭 시도
     * 2) 없으면 임시 Course 생성 후 저장
     */
    private Course resolveOrCreateCourse(TimetableImageImportResponse.Item raw) {
        // 1) courseCode 로 먼저 찾기
        Course found = null;
        String code = raw.getCourseCode();
        if (code != null && !code.isBlank()) {
            found = courseRepository.findByCourseCode(code).orElse(null);
        }

        // 2) 이름 + 요일 + 교시로 한 번 더 매칭 시도
        if (found == null) {
            found = courseRepository
                    .findFirstByNameAndDayOfWeekAndStartPeriodAndEndPeriod(
                            raw.getCourseName(),
                            raw.getDayOfWeek(),
                            raw.getStartPeriod(),
                            raw.getEndPeriod()
                    )
                    .orElse(null);
        }

        if (found != null) {
            return found;
        }

        // 3) 매칭 실패 → 임시 Course 생성
        String tempCode = generateTempCourseCode();

        String category = guessCategory(raw.getStartPeriod());
        int credit = 3; // 기본값, 필요하면 규칙 바꿔도 됨

        Course temp = Course.builder()
                .courseCode(tempCode)
                .name(raw.getCourseName())
                .department("IMPORT")      // 임시 학과
                .category(category)
                .credit(credit)
                .hours(credit)            // 시간 = 학점으로 맞춰두기
                .dayOfWeek(raw.getDayOfWeek())
                .startPeriod(raw.getStartPeriod())
                .endPeriod(raw.getEndPeriod())
                .room(raw.getRoom())
                .professor(null)
                .section("01")
                .recommendedGrade(null)
                .build();

        return courseRepository.save(temp);
    }

    /** 임시 과목 코드 생성 (중복 방지) */
    private String generateTempCourseCode() {
        while (true) {
            String code = "TMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!courseRepository.existsByCourseCode(code)) {
                return code;
            }
        }
    }

    /** startPeriod 기준으로 대충 카테고리 추정 (원하면 더 똑똑하게 바꿔도 됨) */
    private String guessCategory(Integer startPeriod) {
        if (startPeriod == null) return "기타";
        // 예시: 블록(21~26)을 전공, 나머지를 교양으로
        if (startPeriod >= 21 && startPeriod <= 26) {
            return "전필";
        }
        return "교양";
    }
}