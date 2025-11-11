// src/main/java/Hwai_team/UniTime/domain/timetable/controller/TimetableController.java
package Hwai_team.UniTime.domain.timetable.controller;

import Hwai_team.UniTime.domain.timetable.dto.AiTimetableRequest;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableResponse;
import Hwai_team.UniTime.domain.timetable.service.TimetableService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Hidden
@RestController
@RequestMapping("/api/timetables")
@RequiredArgsConstructor
@Tag(name = "Timetable", description = "시간표 조회/관리 및 AI 생성 API")
public class TimetableController {

    private final TimetableService timetableService;

    @Operation(
            summary = "AI로 시간표 생성 (랩핑 엔드포인트)",
            description = """
                userId와 자연어 요청을 받아 AI가 시간표를 생성합니다.
                내부적으로 AiTimetableService를 호출해서 시간표를 만들고,
                간단한 응답 DTO(AiTimetableResponse) 형태로 결과를 반환합니다.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "message": "월수 위주로 18학점 전공 시간표 만들어줘",
                                              "year": 2025,
                                              "semester": 1
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "시간표 생성 성공",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "timetableId": 12,
                                      "title": "2025-1학기 전공 중심 시간표",
                                      "userName": "김민호",
                                      "message": "AI 시간표가 성공적으로 생성되었습니다."
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/ai-generate")
    public ResponseEntity<AiTimetableResponse> generate(
            @RequestParam Long userId,
            @RequestBody AiTimetableRequest request
    ) {
        AiTimetableResponse response = timetableService.generateAiTimetable(userId, request);
        return ResponseEntity.ok(response);
    }


}