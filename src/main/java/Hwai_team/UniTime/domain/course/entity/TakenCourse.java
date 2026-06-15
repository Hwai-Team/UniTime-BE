// src/main/java/.../taken/TakenCourse.java
package Hwai_team.UniTime.domain.course.entity;

import Hwai_team.UniTime.domain.user.entity.User;
import Hwai_team.UniTime.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "taken_courses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","course_id","year","semester"}))
public class TakenCourse extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "course_id", nullable = false)
    private Course course;
}