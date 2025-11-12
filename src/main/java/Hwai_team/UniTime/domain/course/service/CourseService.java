// src/main/java/Hwai_team/UniTime/domain/course/service/CourseService.java
package Hwai_team.UniTime.domain.course.service;

import Hwai_team.UniTime.domain.course.dto.CourseRequest;
import Hwai_team.UniTime.domain.course.dto.CourseResponse;
import Hwai_team.UniTime.domain.course.dto.CourseSearchCond;
import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;   // ✅ 여기가 핵심 import
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<CourseResponse> searchCourses(CourseSearchCond cond) {
        // 기본: 필터 없으면 전체 반환
        if (cond.getGradeYear() == null && cond.getCategory() == null && cond.getKeyword() == null) {
            return courseRepository.findAll()
                    .stream()
                    .map(CourseResponse::from)
                    .collect(Collectors.toList());
        }

        // 조합 필터링
        if (cond.getGradeYear() != null && cond.getCategory() != null) {
            return courseRepository.findByRecommendedGradeAndCategory(cond.getGradeYear(), cond.getCategory())
                    .stream()
                    .map(CourseResponse::from)
                    .collect(Collectors.toList());
        } else if (cond.getGradeYear() != null) {
            return courseRepository.findByRecommendedGrade(cond.getGradeYear())
                    .stream()
                    .map(CourseResponse::from)
                    .collect(Collectors.toList());
        } else if (cond.getKeyword() != null) {
            return courseRepository.findByNameContainingIgnoreCase(cond.getKeyword())
                    .stream()
                    .map(CourseResponse::from)
                    .collect(Collectors.toList());
        }

        return courseRepository.findAll()
                .stream()
                .map(CourseResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다. id=" + id));
        return CourseResponse.from(course);
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        Course course = Course.builder()
                .courseCode(request.getCourseCode())
                .name(request.getName())
                .recommendedGrade(request.getRecommendedGrade())
                .category(request.getCategory())
                .credit(request.getCredit())
                .hours(request.getHours())
                .department(request.getDepartment())
                .dayOfWeek(request.getDayOfWeek())
                .startPeriod(request.getStartPeriod())
                .endPeriod(request.getEndPeriod())
                .professor(request.getProfessor())
                .room(request.getRoom())
                .section(request.getSection())
                .build();

        Course saved = courseRepository.save(course);
        return CourseResponse.from(saved);
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다. id=" + id));

        course.updateFromRequest(request);
        return CourseResponse.from(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    // 확장용 (페이징 포함 검색)
    @Transactional(readOnly = true)
    public Page<CourseResponse> getCourses(
            String q, String department, String category,
            Integer grade, Integer credit, String dayOfWeek,
            int page, int size, Sort sort
    ) {
        Pageable pageable = PageRequest.of(page, size, sort);
        return courseRepository.search(q, department, category, grade, credit, dayOfWeek, pageable)
                .map(CourseResponse::from);
    }
}