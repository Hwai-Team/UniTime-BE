package Hwai_team.UniTime.domain.timetable.repository;

import Hwai_team.UniTime.domain.timetable.entity.AiTimetable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiTimetableRepository extends JpaRepository<AiTimetable, Long> {

    // 유저당 하나만 관리할 거라 이렇게 사용
    Optional<AiTimetable> findByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);
}