// src/main/java/Hwai_team/UniTime/domain/course/service/CourseService.java
package Hwai_team.UniTime.domain.course.service;

import Hwai_team.UniTime.domain.course.dto.*;
import Hwai_team.UniTime.domain.course.entity.Course;
import Hwai_team.UniTime.domain.course.entity.TakenCourse;
import Hwai_team.UniTime.domain.course.repository.CourseRepository;
import Hwai_team.UniTime.domain.course.repository.TakenCourseRepository;
import Hwai_team.UniTime.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final UserRepository userRepository;
    private final TakenCourseRepository takenCourseRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<CourseResponse> searchCourses(CourseSearchCond cond) {
        // 필터 전혀 없으면 전체
        if (cond.getGradeYear() == null && cond.getCategory() == null && cond.getKeyword() == null) {
            return courseRepository.findAll()
                    .stream()
                    .map(CourseResponse::from)
                    .collect(Collectors.toList());
        }

        // 학년 + 이수구분
        if (cond.getGradeYear() != null && cond.getCategory() != null) {
            return courseRepository.findByRecommendedGradeAndCategory(
                            cond.getGradeYear(), cond.getCategory()
                    )
                    .stream()
                    .map(CourseResponse::from)
                    .collect(Collectors.toList());
        }
        // 학년만
        else if (cond.getGradeYear() != null) {
            return courseRepository.findByRecommendedGrade(cond.getGradeYear())
                    .stream()
                    .map(CourseResponse::from)
                    .collect(Collectors.toList());
        }
        // 키워드 검색
        else if (cond.getKeyword() != null) {
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
                .recommendedGrade(request.getRecommendedGrade()
                )
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

    // 확장용 (페이징 검색)
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

    /**
     * 이전 수강 과목 토글
     */
    @Transactional
    public TakenCourseToggleResponse toggleTaken(Long courseId, TakenCourseToggleRequest req) {
        if (req.getUserId() == null || req.getTaken() == null) {
            throw new IllegalArgumentException("userId와 taken은 필수입니다.");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과목입니다. id=" + courseId));
        var user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + req.getUserId()));

        var existing = takenCourseRepository
                .findByUser_IdAndCourse_Id(req.getUserId(), courseId);

        if (Boolean.TRUE.equals(req.getTaken())) {
            // 없으면 새로 저장
            if (existing.isEmpty()) {
                takenCourseRepository.save(
                        TakenCourse.builder()
                                .user(user)
                                .course(course)
                                .build()
                );
            }
            return new TakenCourseToggleResponse(courseId, req.getUserId(), true);
        } else {
            // 있으면 삭제
            existing.ifPresent(tc ->
                    takenCourseRepository.deleteByUser_IdAndCourse_Id(
                            req.getUserId(), courseId
                    )
            );
            return new TakenCourseToggleResponse(courseId, req.getUserId(), false);
        }
    }

    @Transactional(readOnly = true)
    public List<Long> getTakenCourseIds(Long userId) {
        return takenCourseRepository.findByUser_Id(userId)
                .stream()
                .map(tc -> tc.getCourse().getId())
                .toList();
    }
}