// src/main/java/Hwai_team/UniTime/domain/course/controller/CourseController.java
package Hwai_team.UniTime.domain.course.controller;

import Hwai_team.UniTime.domain.course.dto.CourseRequest;
import Hwai_team.UniTime.domain.course.dto.CourseResponse;
import Hwai_team.UniTime.domain.course.dto.CourseSearchCond;
import Hwai_team.UniTime.domain.course.service.CourseService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "교과목(강의) 조회/관리 API")
public class CourseController {

    private final CourseService courseService;

    // =======================
    // 목록 조회 (Swagger 노출)
    // =======================
    @Operation(
            summary = "개설 강의 목록 조회",
            description = "간단 검색/필터 지원.\n" +
                    "- gradeYear: 추천 학년(정수)\n" +
                    "- category: 전선/전필/교선/교필 등\n" +
                    "- keyword: 과목명·코드·교수명 부분검색"
    )
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CourseResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    [
                                      {
                                        "id": 123,
                                        "courseCode": "EN1005",
                                        "name": "JAVA프로그래밍",
                                        "recommendedGrade": 1,
                                        "category": "전선",
                                        "credit": 3,
                                        "hours": 3,
                                        "department": "전자컴",
                                        "dayOfWeek": "MON",
                                        "startPeriod": 1,
                                        "endPeriod": 2,
                                        "professor": "김교수",
                                        "room": "복-320",
                                        "section": "01"
                                      }
                                    ]
                                    """
                    )
            )
    )
    @GetMapping
    public ResponseEntity<List<CourseResponse>> searchCourses(
            @Parameter(description = "추천 학년 예) 1,2,3,4")
            @RequestParam(required = false) Integer gradeYear,
            @Parameter(description = "전선/전필/교선/교필 등")
            @RequestParam(required = false) String category,
            @Parameter(description = "과목명/코드/교수명 키워드")
            @RequestParam(required = false) String keyword
    ) {
        CourseSearchCond cond = new CourseSearchCond();
        cond.setGradeYear(gradeYear);
        cond.setCategory(category);
        cond.setKeyword(keyword);

        return ResponseEntity.ok(courseService.searchCourses(cond));
    }

    // =======================
    // 단건 조회 (Swagger 노출)
    // =======================
    @Operation(summary = "강의 상세 조회", description = "과목 ID로 단건 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourse(id));
    }

    // =======================
    // 아래 3개는 운영용(Swagger 숨김)
    // =======================
    @Hidden
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@RequestBody CourseRequest request) {
        return ResponseEntity.ok(courseService.createCourse(request));
    }

    @Hidden
    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long id,
            @RequestBody CourseRequest request
    ) {
        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    @Hidden
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}