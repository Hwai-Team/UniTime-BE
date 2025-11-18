// src/main/java/Hwai_team/UniTime/domain/timetable/entity/Timetable.java
package Hwai_team.UniTime.domain.timetable.entity;

import Hwai_team.UniTime.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "timetables")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Timetable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    private Integer year;
    private Integer semester;
    private String title;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TimetableItem> items = new ArrayList<>();

    public void changeTitle(String title) {
        this.title = title;
    }

    /**
     * 시간표에 아이템 추가할 때
     * 같은 요일 + 교시가 겹치면 예외를 던져서 무조건 막는다.
     */
    public void addItem(TimetableItem newItem) {
        if (newItem == null) return;

        if (this.items == null) {
            this.items = new ArrayList<>();
        }

        // dayOfWeek / period 정보가 있을 때만 겹침 체크
        if (newItem.getDayOfWeek() != null &&
                newItem.getStartPeriod() != null &&
                newItem.getEndPeriod() != null) {

            for (TimetableItem it : this.items) {
                if (it.getDayOfWeek() == null ||
                        it.getStartPeriod() == null ||
                        it.getEndPeriod() == null) {
                    continue;
                }

                // 요일 다르면 패스
                if (!it.getDayOfWeek().equals(newItem.getDayOfWeek())) {
                    continue;
                }

                int aStart = it.getStartPeriod();
                int aEnd   = it.getEndPeriod();
                int bStart = newItem.getStartPeriod();
                int bEnd   = newItem.getEndPeriod();

                // [aStart, aEnd] 와 [bStart, bEnd] 가 하나라도 겹치면 충돌
                boolean overlap = (aStart <= bEnd) && (bStart <= aEnd);
                if (overlap) {
                    throw new IllegalStateException(
                            "시간표 충돌: " +
                                    displayName(it) + " ↔ " + displayName(newItem) +
                                    " (" + it.getDayOfWeek() + " " + aStart + "–" + aEnd + "교시)"
                    );
                }
            }
        }

        this.items.add(newItem);
        newItem.setTimetable(this);
    }

    private String displayName(TimetableItem item) {
        if (item.getCourseName() != null) return item.getCourseName();
        if (item.getCourse() != null && item.getCourse().getName() != null) {
            return item.getCourse().getName();
        }
        return "";
    }
}