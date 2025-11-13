package Hwai_team.UniTime.domain.timetable.controller;

import Hwai_team.UniTime.domain.timetable.dto.AiGenerateButtonRequest;
import Hwai_team.UniTime.domain.timetable.dto.AiGenerateButtonResponse;
import Hwai_team.UniTime.domain.timetable.service.TimetableAiIntentService;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/timetables/ai-generate")
@RequiredArgsConstructor
public class TimetableAiGenerateController {

    private final TimetableAiIntentService timetableAiIntentService;

    @PostMapping("/button-visibility")
    public ResponseEntity<AiGenerateButtonResponse> checkButtonVisibility(
            @RequestBody AiGenerateButtonRequest request
    ) {
        AiGenerateButtonResponse response =
                timetableAiIntentService.checkButtonVisibility(request);
        return ResponseEntity.ok(response);
    }
}