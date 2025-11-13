// src/main/java/Hwai_team/UniTime/domain/course/repository/TakenCourseRepository.java
package Hwai_team.UniTime.domain.course.repository;

import Hwai_team.UniTime.domain.course.entity.TakenCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TakenCourseRepository extends JpaRepository<TakenCourse, Long> {

    Optional<TakenCourse> findByUser_IdAndCourse_Id(Long userId, Long courseId);

    void deleteByUser_IdAndCourse_Id(Long userId, Long courseId);

    List<TakenCourse> findByUser_Id(Long userId);
}