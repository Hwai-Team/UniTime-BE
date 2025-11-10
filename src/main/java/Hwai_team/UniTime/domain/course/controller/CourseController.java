// src/main/java/Hwai_team/UniTime/domain/course/controller/CourseController.java
package Hwai_team.UniTime.domain.course.controller;

import Hwai_team.UniTime.domain.course.dto.CourseRequest;
import Hwai_team.UniTime.domain.course.dto.CourseResponse;
import Hwai_team.UniTime.domain.course.dto.CourseSearchCond;
import Hwai_team.UniTime.domain.course.service.CourseService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
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

    // 🔒 Swagger 에서 숨김
    @Hidden
    @GetMapping
    public ResponseEntity<List<CourseResponse>> searchCourses(
            @RequestParam(required = false) Integer gradeYear,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        CourseSearchCond cond = new CourseSearchCond();
        cond.setGradeYear(gradeYear);
        cond.setCategory(category);
        cond.setKeyword(keyword);

        return ResponseEntity.ok(courseService.searchCourses(cond));
    }

    // ✅ 이놈만 Swagger에 노출
    @Operation(summary = "단일 교과목 조회", description = "과목 ID로 단건 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourse(id));
    }

    // 🔒 Swagger 에서 숨김
    @Hidden
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@RequestBody CourseRequest request) {
        return ResponseEntity.ok(courseService.createCourse(request));
    }

    // 🔒 Swagger 에서 숨김
    @Hidden
    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long id,
            @RequestBody CourseRequest request
    ) {
        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    // 🔒 Swagger 에서 숨김
    @Hidden
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}