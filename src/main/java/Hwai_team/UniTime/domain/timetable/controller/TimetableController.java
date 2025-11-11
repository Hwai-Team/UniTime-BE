// src/main/java/Hwai_team/UniTime/domain/timetable/controller/TimetableController.java
package Hwai_team.UniTime.domain.timetable.controller;

import Hwai_team.UniTime.domain.timetable.dto.*;
import Hwai_team.UniTime.domain.timetable.service.TimetableService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;



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

    // === 1) 내 시간표 목록 조회 ===
    @Operation(
            summary = "내 시간표 목록 조회",
            description = "특정 유저가 가진 전체 시간표 목록을 반환합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<List<TimetableResponse>> getMyTimetables(
            @RequestParam Long userId
    ) {
        List<TimetableResponse> list = timetableService.getMyTimetables(userId);
        return ResponseEntity.ok(list);
    }

    // === 2) 시간표 생성 ===
    @Operation(
            summary = "시간표 생성",
            description = "연도/학기를 선택해서 빈 시간표를 생성합니다."
    )
    @RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = TimetableCreateRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "year": 2024,
                                      "semester": 1,
                                      "title": "24학년도 1학기"
                                    }
                                    """
                    )
            )
    )
    @PostMapping
    public ResponseEntity<TimetableResponse> createTimetable(
            @RequestParam Long userId,
            @org.springframework.web.bind.annotation.RequestBody TimetableCreateRequest request
    ) {
        TimetableResponse response = timetableService.createTimetable(userId, request);
        return ResponseEntity.ok(response);
    }

    // === 3) 시간표 수정 ===
    @Operation(
            summary = "시간표 수정",
            description = "시간표 제목 및 과목 리스트를 통째로 수정합니다."
    )
    @RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = TimetableUpdateRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "title": "24학년도 1학기 수정",
                                      "items": [
                                        {
                                          "courseName": "자료구조",
                                          "dayOfWeek": "MON",
                                          "startPeriod": 1,
                                          "endPeriod": 2,
                                          "room": "IT-101",
                                          "category": "전공"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @PutMapping("/{timetableId}")
    public ResponseEntity<TimetableResponse> updateTimetable(
            @PathVariable Long timetableId,
            @org.springframework.web.bind.annotation.RequestBody TimetableUpdateRequest request
    ) {
        TimetableResponse response = timetableService.updateTimetable(timetableId, request);
        return ResponseEntity.ok(response);
    }

    // === 4) 시간표 삭제 ===
    @Operation(
            summary = "시간표 삭제",
            description = "특정 시간표와 그 안의 과목들을 모두 삭제합니다."
    )
    @DeleteMapping("/{timetableId}")
    public ResponseEntity<Void> deleteTimetable(
            @PathVariable Long timetableId
    ) {
        timetableService.deleteTimetable(timetableId);
        return ResponseEntity.noContent().build();
    }
}