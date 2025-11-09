// src/main/java/Hwai_team/UniTime/domain/user/entity/User.java
package Hwai_team.UniTime.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;    // 해시 저장

    @Column(nullable = false, length = 50)
    private String studentId;   // 학번

    @Column(nullable = false, length = 50)
    private String department;  // 학과

    @Column(nullable = false)
    private Integer grade;      // 학년

    @Column(nullable = false, length = 50)
    private String name;        // 이름/닉네임
}