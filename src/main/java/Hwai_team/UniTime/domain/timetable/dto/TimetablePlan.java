// src/main/java/Hwai_team/UniTime/domain/timetable/dto/TimetablePlan.java
package Hwai_team.UniTime.domain.timetable.dto;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetablePlan {

    private String title;

    @Builder.Default
    private List<TimetableItemPlan> items = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimetableItemPlan {
        private String courseCode;
        private String courseName;
        private String dayOfWeek;   // "MON" ...
        private String startTime;   // "09:00"
        private String endTime;     // "10:00"
        private String location;
        private String priority;    // "MAJOR" / "ELECTIVE" / "OPTIONAL"
    }
}