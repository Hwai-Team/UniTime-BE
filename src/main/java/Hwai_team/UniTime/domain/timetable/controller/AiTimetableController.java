package Hwai_team.UniTime.domain.timetable.controller;

import Hwai_team.UniTime.domain.timetable.dto.AiTimetableRequest;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableResponse;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableSaveRequest;
import Hwai_team.UniTime.domain.timetable.entity.Timetable;
import Hwai_team.UniTime.domain.timetable.service.AiTimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timetables/ai") // ✅ 복수형 + timetableApis 그룹에 딱 걸리게
@RequiredArgsConstructor
@Tag(name = "AI Timetable", description = "AI 기반 시간표 생성 / 저장 / 조회 API")
public class AiTimetableController {

    private final AiTimetableService aiTimetableService;

    // =======================
    // 1) AI 시간표 생성
    // =======================
    @Operation(
            summary = "AI 시간표 생성",
            description = """
                사용자의 자연어 요청을 기반으로 AI가 시간표를 자동 생성합니다.
                예: "월수 위주로 전공 18학점 시간표 만들어줘"
                """,
            requestBody = @RequestBody(
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

    // =======================
    // 2) AI 시간표 저장 (생성 결과를 DB에 기록)
    // =======================
    @Operation(
            summary = "AI 시간표 저장/수정",
            description = """
                    이미 생성된 시간표를 'AI 시간표'로 저장합니다.
                    프론트 플로우:
                    1) POST /api/timetables/ai 로 시간표 생성
                    2) 생성된 timetable.id 를 들고 와서
                    3) PUT /api/timetables/ai 로 userId + timetableId 보내서 저장
                    
                    resultSummary 는 선택(옵션) 필드입니다.
                    """
            ,
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AiTimetableSaveRequest.class),
                            examples = @ExampleObject(
                                    name = "AI 시간표 저장 예시",
                                    value = """
                                            {
                                              "userId": 1,
                                              "timetableId": 12,
                                              "resultSummary": "월수 오전 위주의 18학점 전공 시간표입니다."
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "저장 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AiTimetableResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 3,
                                              "userId": 1,
                                              "userName": "김민호",
                                              "timetableId": 12,
                                              "title": "2025-1학기 전공 중심 시간표",
                                              "resultSummary": "월수 오전 위주의 18학점 전공 시간표입니다.",
                                              "createdAt": "2025-11-11T12:34:56"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @ApiResponse(responseCode = "404", description = "유저 또는 시간표를 찾을 수 없음")
    })
    @PutMapping // ✅ PUT /api/timetables/ai
    public ResponseEntity<AiTimetableResponse> saveAiTimetable(
            @org.springframework.web.bind.annotation.RequestBody AiTimetableSaveRequest request
    ) {
        AiTimetableResponse response = aiTimetableService.saveAiTimetable(request);
        return ResponseEntity.ok(response);
    }

    // =======================
    // 3) AI 시간표 조회
    // =======================
    @Operation(
            summary = "AI 시간표 조회",
            description = "특정 유저가 저장한 AI 시간표(유저당 1개 기준)를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = AiTimetableResponse.class))
    )
    @GetMapping // ✅ GET /api/timetables/ai?userId=1
    public ResponseEntity<AiTimetableResponse> getAiTimetable(
            @Parameter(description = "조회할 유저 ID", example = "1")
            @RequestParam Long userId
    ) {
        AiTimetableResponse response = aiTimetableService.getAiTimetable(userId);
        return ResponseEntity.ok(response);
    }

    // =======================
    // 4) AI 시간표 삭제
    // =======================
    @Operation(
            summary = "AI 시간표 삭제",
            description = "특정 유저가 저장한 AI 시간표 기록을 삭제합니다."
    )
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @DeleteMapping // ✅ DELETE /api/timetables/ai?userId=1
    public ResponseEntity<Void> deleteAiTimetable(
            @Parameter(description = "삭제할 유저 ID", example = "1")
            @RequestParam Long userId
    ) {
        aiTimetableService.deleteAiTimetable(userId);
        return ResponseEntity.noContent().build();
    }
}