// src/main/java/.../taken/TakenCourse.java
package Hwai_team.UniTime.domain.course.entity;

import Hwai_team.UniTime.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "taken_courses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","course_id","year","semester"}))
public class TakenCourse {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist void onCreate(){ createdAt = LocalDateTime.now(); }
}