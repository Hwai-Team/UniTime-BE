package Hwai_team.UniTime.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "graduation_year")
    private Integer graduationYear;  // ✅ 한 번만 선언

    // @Getter가 있으니 따로 getter/setter는 없어도 됨.
    // 만약 수정용 setter 필요하면 아래만 남겨도 됨.
    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }
}