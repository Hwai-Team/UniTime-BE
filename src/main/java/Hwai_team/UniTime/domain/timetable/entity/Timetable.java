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
    @Builder.Default                      // 🔥 중요
    private List<TimetableItem> items = new ArrayList<>();   // 🔥 여기서 바로 초기화

    public void changeTitle(String title) {
        this.title = title;
    }

    public void addItem(TimetableItem item) {
        // 혹시 모를 방어 코드
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        item.setTimetable(this);
    }
}