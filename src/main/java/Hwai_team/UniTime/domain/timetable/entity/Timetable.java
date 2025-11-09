// src/main/java/Hwai_team/UniTime/domain/timetable/entity/Timetable.java
package Hwai_team.UniTime.domain.timetable.entity;

import Hwai_team.UniTime.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "timetables")
public class Timetable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 이 시간표의 주인 (한 유저는 여러 시간표를 가질 수 있음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    // 🔹 시간표 제목
    @Column(name = "title", nullable = false)
    private String title;



    // 🔹 학년도 / 학기
    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer semester;

    // 🔹 생성 및 수정 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 🔹 시간표 내 아이템 목록 (양방향 관계)
    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TimetableItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 🔹 편의 메서드
    public void addItem(TimetableItem item) {
        items.add(item);
        item.setTimetable(this);
    }
}