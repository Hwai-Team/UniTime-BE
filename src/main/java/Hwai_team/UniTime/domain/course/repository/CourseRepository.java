// src/main/java/Hwai_team/UniTime/domain/course/repository/CourseRepository.java
package Hwai_team.UniTime.domain.course.repository;

import Hwai_team.UniTime.domain.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCode(String courseCode);

    Optional<Course> findByCourseCodeAndSection(String courseCode, String section);

    Optional<Course> findByName(String name);

    boolean existsByCourseCode(String courseCode);

    // ✅ 이름 + 요일 + 교시로 과목 매칭 (이미지 import용)
    Optional<Course> findFirstByNameAndDayOfWeekAndStartPeriodAndEndPeriod(
            String name,
            String dayOfWeek,
            Integer startPeriod,
            Integer endPeriod
    );

    @Query("""
        SELECT c FROM Course c
        WHERE (:q IS NULL OR 
               LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(COALESCE(c.professor, '')) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:department IS NULL OR c.department = :department)
          AND (:category   IS NULL OR c.category   = :category)
          AND (:recommendedGrade IS NULL OR c.recommendedGrade = :recommendedGrade)
          AND (:credit     IS NULL OR c.credit    = :credit)
          AND (:dayOfWeek  IS NULL OR c.dayOfWeek = :dayOfWeek)
        """)
    Page<Course> search(
            @Param("q") String q,
            @Param("department") String department,
            @Param("category") String category,
            @Param("recommendedGrade") Integer recommendedGrade,
            @Param("credit") Integer credit,
            @Param("dayOfWeek") String dayOfWeek,
            Pageable pageable
    );
}