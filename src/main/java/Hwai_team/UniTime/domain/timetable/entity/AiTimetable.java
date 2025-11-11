package Hwai_team.UniTime.domain.timetable.entity;

import Hwai_team.UniTime.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ai_timetables")
public class AiTimetable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 유저: AI 시간표 = 1 : N 이긴 한데,
    // 서비스 레벨에서 "유저당 1개만 유지"로 관리할 거라 그대로 둔다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // AI가 시간표 만들 때 사용한 프롬프트(조건)
    // 생성 API에서만 세팅하고, 이후 저장 API에서는 건들지 않을 예정
    @Column(columnDefinition = "TEXT")
    private String prompt;

    // "전공 3개, 교양 2개, 총 18학점" 이런 요약
    @Column(columnDefinition = "TEXT")
    private String resultSummary;

    // 실제 저장된 Timetable 엔티티와 연결
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id")
    private Timetable timetable;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ---- 편의 메서드 ----
    /**
     * ✅ 저장/수정 API에서 사용할 메서드
     *  - prompt는 그대로 두고
     *  - resultSummary, timetable만 업데이트
     */
    public void update(String resultSummary, Timetable timetable) {
        this.resultSummary = resultSummary;
        this.timetable = timetable;
    }
}