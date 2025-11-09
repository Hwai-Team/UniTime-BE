package Hwai_team.UniTime.domain.timetable.repository;

import Hwai_team.UniTime.domain.timetable.entity.TimetableItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimetableItemRepository extends JpaRepository<TimetableItem, Long> {
}