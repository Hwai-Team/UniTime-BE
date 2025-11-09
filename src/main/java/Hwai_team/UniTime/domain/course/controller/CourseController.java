// src/main/java/Hwai_team/UniTime/domain/course/controller/CourseController.java
package Hwai_team.UniTime.domain.course.controller;

import Hwai_team.UniTime.domain.course.dto.CourseRequest;
import Hwai_team.UniTime.domain.course.dto.CourseResponse;
import Hwai_team.UniTime.domain.course.dto.CourseSearchCond;
import Hwai_team.UniTime.domain.course.service.CourseService;
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

    @Operation(
            summary = "교과목 목록 조회",
            description = """
                    학년, 이수구분, 키워드로 과목을 필터링해서 조회합니다.
                    파라미터를 아무것도 안 주면 전체를 반환합니다.
                    """)
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CourseResponse.class))
    )
    @GetMapping
    public ResponseEntity<List<CourseResponse>> searchCourses(
            @Parameter(description = "학년 (1,2,3,4)", example = "2")
            @RequestParam(required = false) Integer gradeYear,
            @Parameter(description = "이수구분 (전선, 전필, 교선 등)", example = "전선")
            @RequestParam(required = false) String category,
            @Parameter(description = "과목명 키워드", example = "자료구조")
            @RequestParam(required = false) String keyword
    ) {
        CourseSearchCond cond = new CourseSearchCond();
        cond.setGradeYear(gradeYear);
        cond.setCategory(category);
        cond.setKeyword(keyword);

        return ResponseEntity.ok(courseService.searchCourses(cond));
    }

    @Operation(summary = "단일 교과목 조회", description = "과목 ID로 단건 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourse(id));
    }

    @Operation(
            summary = "교과목 등록 (관리자용)",
            description = "CSV로 안 넣고 API로 직접 과목을 등록하고 싶을 때 사용합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "gradeYear": 2,
                                              "category": "전선",
                                              "courseCode": "CSE201",
                                              "name": "자료구조",
                                              "section": "01",
                                              "credit": 3,
                                              "professor": "김교수",
                                              "dayOfWeek": "MON",
                                              "startPeriod": 3,
                                              "endPeriod": 4,
                                              "room": "공학관 101"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "등록 성공")
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@RequestBody CourseRequest request) {
        return ResponseEntity.ok(courseService.createCourse(request));
    }

    @Operation(summary = "교과목 수정 (관리자용)")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long id,
            @RequestBody CourseRequest request
    ) {
        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    @Operation(summary = "교과목 삭제 (관리자용)")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}