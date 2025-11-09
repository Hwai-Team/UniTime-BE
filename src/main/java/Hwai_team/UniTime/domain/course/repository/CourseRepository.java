// src/main/java/Hwai_team/UniTime/domain/course/repository/CourseRepository.java
package Hwai_team.UniTime.domain.course.repository;

import Hwai_team.UniTime.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCode(String courseCode);

    Optional<Course> findByCourseCodeAndSection(String courseCode, String section);

    List<Course> findByGradeYear(Integer gradeYear);

    List<Course> findByGradeYearAndCategory(Integer gradeYear, String category);

    List<Course> findByNameContainingIgnoreCase(String keyword);
}