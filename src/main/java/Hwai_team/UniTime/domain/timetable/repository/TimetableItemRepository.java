// src/main/java/Hwai_team/UniTime/domain/timetable/repository/TimetableItemRepository.java
package Hwai_team.UniTime.domain.timetable.repository;

import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.timetable.entity.TimetableItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableItemRepository extends JpaRepository<TimetableItem, Long> {

    void deleteByTimetable_Id(Long timetableId);

}