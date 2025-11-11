// src/main/java/Hwai_team/UniTime/domain/timetable/repository/TimetableRepository.java
package Hwai_team.UniTime.domain.timetable.repository;

import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    // 특정 유저의 시간표 목록 (최신 학기 순)
    List<Timetable> findByOwner_IdOrderByYearDescSemesterDesc(Long ownerId);

    // 같은 연도/학기 시간표가 이미 있는지 체크 (필요하면 사용)
    boolean existsByOwner_IdAndYearAndSemester(Long ownerId, Integer year, Integer semester);
}