// src/main/java/Hwai_team/UniTime/domain/timetable/entity/Timetable.java
package Hwai_team.UniTime.domain.timetable.entity;

import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "timetables")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Timetable extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    private Integer year;
    private Integer semester;
    private String title;

    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TimetableItem> items = new ArrayList<>();

    public void changeTitle(String title) {
        this.title = title;
    }

    /**
     * 시간표에 아이템 추가할 때
     * 같은 요일 + 실제 시간대(분 단위)가 겹치면 예외를 던져서 무조건 막는다.
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

                // 🔥 교시 → 실제 시간(분)으로 변환
                int aStart = periodStartMinutes(it.getStartPeriod());
                int aEnd   = periodEndMinutes(it.getEndPeriod());
                int bStart = periodStartMinutes(newItem.getStartPeriod());
                int bEnd   = periodEndMinutes(newItem.getEndPeriod());

                // 🔥 [aStart, aEnd) 와 [bStart, bEnd) 가 1분이라도 겹치면 충돌
                boolean overlap = (aStart < bEnd) && (bStart < aEnd);
                if (overlap) {
                    throw new IllegalStateException(
                            "시간표 충돌: " +
                                    displayName(it) + " ↔ " + displayName(newItem) +
                                    " (" + it.getDayOfWeek() + " " +
                                    it.getStartPeriod() + "–" + it.getEndPeriod() + "교시)"
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

    /** 교시 시작 시간을 분(minute) 단위로 변환 */
    private static int periodStartMinutes(int period) {
        switch (period) {
            case 1:  return 9 * 60;          // 09:00
            case 2:  return 10 * 60;         // 10:00
            case 3:  return 11 * 60;         // 11:00
            case 4:  return 12 * 60;         // 12:00
            case 5:  return 13 * 60;         // 13:00
            case 6:  return 14 * 60;         // 14:00
            case 7:  return 15 * 60;         // 15:00
            case 8:  return 16 * 60;         // 16:00
            case 9:  return 17 * 60;         // 17:00

            case 21: return 9 * 60;          // 09:00
            case 22: return 10 * 60 + 30;    // 10:30
            case 23: return 12 * 60;         // 12:00
            case 24: return 13 * 60 + 30;    // 13:30
            case 25: return 15 * 60;         // 15:00
            case 26: return 16 * 60 + 30;    // 16:30
            default:
                throw new IllegalArgumentException("알 수 없는 교시: " + period);
        }
    }

    /** 교시 종료 시간을 분(minute) 단위로 변환 */
    private static int periodEndMinutes(int period) {
        switch (period) {
            case 1:  return 9 * 60 + 50;     // 09:50
            case 2:  return 10 * 60 + 50;    // 10:50
            case 3:  return 11 * 60 + 50;    // 11:50
            case 4:  return 12 * 60 + 50;    // 12:50
            case 5:  return 13 * 60 + 50;    // 13:50
            case 6:  return 14 * 60 + 50;    // 14:50
            case 7:  return 15 * 60 + 50;    // 15:50
            case 8:  return 16 * 60 + 50;    // 16:50
            case 9:  return 17 * 60 + 50;    // 17:50

            case 21: return 10 * 60 + 15;    // 10:15
            case 22: return 11 * 60 + 45;    // 11:45
            case 23: return 13 * 60 + 15;    // 13:15
            case 24: return 14 * 60 + 45;    // 14:45
            case 25: return 16 * 60 + 15;    // 16:15
            case 26: return 17 * 60 + 45;    // 17:45
            default:
                throw new IllegalArgumentException("알 수 없는 교시: " + period);
        }
    }
}