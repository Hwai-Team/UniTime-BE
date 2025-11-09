// src/main/java/Hwai_team/UniTime/domain/course/service/CourseService.java
package Hwai_team.UniTime.domain.course.service;

import Hwai_team.UniTime.domain.course.dto.CourseRequest;
import Hwai_team.UniTime.domain.course.dto.CourseResponse;
import Hwai_team.UniTime.domain.course.dto.CourseSearchCond;
import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    // 전체 + 조건 검색
    public List<CourseResponse> searchCourses(CourseSearchCond cond) {
        List<Course> courses;

        // 조건 조합 간단하게 처리
        if (cond.getGradeYear() != null && cond.getCategory() != null) {
            courses = courseRepository.findByGradeYearAndCategory(
                    cond.getGradeYear(), cond.getCategory()
            );
        } else if (cond.getGradeYear() != null) {
            courses = courseRepository.findByGradeYear(cond.getGradeYear());
        } else if (cond.getKeyword() != null && !cond.getKeyword().isBlank()) {
            courses = courseRepository.findByNameContainingIgnoreCase(cond.getKeyword());
        } else {
            courses = courseRepository.findAll();
        }

        return courses.stream()
                .map(CourseResponse::new)
                .toList();
    }

    public CourseResponse getCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과목입니다. id=" + id));
        return new CourseResponse(course);
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        Course course = Course.builder()
                .gradeYear(request.getGradeYear())
                .category(request.getCategory())
                .courseCode(request.getCourseCode())
                .name(request.getName())
                .section(request.getSection())
                .credit(request.getCredit())
                .professor(request.getProfessor())
                .dayOfWeek(request.getDayOfWeek())
                .startPeriod(request.getStartPeriod())
                .endPeriod(request.getEndPeriod())
                .room(request.getRoom())
                .build();

        return new CourseResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과목입니다. id=" + id));

        // setter 안 만들었으니, 빌더 패턴으로 교체하거나, 엔티티에 변경 메서드 만들어도 됨.
        // 여기서는 간단하게 엔티티에 값 세팅하는 메서드 하나 추가했다고 가정할게.
        course.updateFromRequest(request);

        return new CourseResponse(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }
}