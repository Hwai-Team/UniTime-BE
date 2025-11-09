// src/main/java/Hwai_team/UniTime/domain/timetable/repository/TimetableRepository.java
package Hwai_team.UniTime.domain.timetable.repository;

import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findAllByOwner(User owner);
}