// src/main/java/Hwai_team/UniTime/domain/timetable/repository/AiTimetableRepository.java
package Hwai_team.UniTime.domain.timetable.repository;

import Hwai_team.UniTime.domain.timetable.entity.AiTimetable;
import Hwai_team.UniTime.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiTimetableRepository extends JpaRepository<AiTimetable, Long> {

    // 특정 사용자가 요청한 AI 시간표 기록들 조회
    List<AiTimetable> findAllByUser(User user);
}