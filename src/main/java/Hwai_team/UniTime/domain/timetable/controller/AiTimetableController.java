package Hwai_team.UniTime.domain.timetable.controller;

import Hwai_team.UniTime.domain.timetable.dto.AiTimetableRequest;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.timetable.service.AiTimetableService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timetables/ai") // ✅ 복수형 + timetableApis 그룹에 딱 걸리게
@RequiredArgsConstructor
@Tag(name = "AI Timetable", description = "AI 기반 시간표 자동 생성 API")
public class AiTimetableController {

    private final AiTimetableService aiTimetableService;

    @Operation(
            summary = "AI 시간표 생성",
            description = """
                사용자의 자연어 요청을 기반으로 AI가 시간표를 자동 생성합니다.
                예: "월수 위주로 전공 18학점 시간표 만들어줘"
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AiTimetableRequest.class),
                            examples = @ExampleObject(
                                    name = "AI 시간표 생성 예시",
                                    value = """
                                            {
                                              "userId": 1,
                                              "message": "월수 위주로 18학점 전공 시간표 만들어줘",
                                              "year": 2025,
                                              "semester": 1
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "시간표 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Timetable.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 12,
                                              "title": "2025-1학기 전공 중심 시간표",
                                              "items": [
                                                {
                                                  "courseName": "자료구조",
                                                  "dayOfWeek": "MON",
                                                  "startPeriod": 1,
                                                  "endPeriod": 2,
                                                  "room": "공학관 101",
                                                  "category": "전공"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
            @ApiResponse(responseCode = "500", description = "AI 처리 중 오류 발생")
    })
    @PostMapping // ✅ POST /api/timetables/ai
    public ResponseEntity<Timetable> createByAi(
            @org.springframework.web.bind.annotation.RequestBody AiTimetableRequest request
    ) {
        Timetable timetable = aiTimetableService.createByAi(request);
        return ResponseEntity.ok(timetable);
    }
}