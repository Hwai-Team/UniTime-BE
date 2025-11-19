// src/main/java/Hwai_team/UniTime/domain/timetable/controller/TimetableController.java
package Hwai_team.UniTime.domain.timetable.controller;

import Hwai_team.UniTime.domain.timetable.dto.*;
import Hwai_team.UniTime.domain.timetable.service.TimetableImageImportService;
import Hwai_team.UniTime.domain.timetable.service.TimetableService;
import Hwai_team.UniTime.domain.timetable.service.AiTimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/timetables")
@RequiredArgsConstructor
@Tag(name = "Timetable", description = "시간표 조회/관리 및 AI 생성 API")
public class TimetableController {

    private final TimetableService timetableService;
    private final TimetableImageImportService timetableImageImportService;
    private final AiTimetableService aiTimetableService;

    // === 1) 내 시간표 목록 조회 ===
    @Operation(
            summary = "내 시간표 목록 조회",
            description = "특정 유저가 가진 전체 시간표 목록을 반환합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<List<TimetableResponse>> getMyTimetables(@RequestParam Long userId) {
        List<TimetableResponse> list = timetableService.getMyTimetables(userId);
        return ResponseEntity.ok(list);
    }

    // === 2) 일반 시간표 생성 ===
    @Operation(
            summary = "시간표 생성",
            description = "연도/학기를 선택해서 빈 시간표를 생성합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
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
    )
    @PostMapping
    public ResponseEntity<TimetableResponse> createTimetable(
            @RequestParam Long userId,
            @RequestBody TimetableCreateRequest request
    ) {
        TimetableResponse response = timetableService.createTimetable(userId, request);
        return ResponseEntity.ok(response);
    }

    // === 3) 시간표 수정 ===
    @Operation(
            summary = "시간표 내 강의 추가/수정",
            description = "제목 변경 및 과목 리스트 전체 교체",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TimetableUpdateRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "title": "24학년도 1학기 수정",
                                              "items": [
                                                { "courseId": 73 },
                                                { "courseId": 74 },
                                                { "courseId": 75 }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    )
    @PutMapping("/{timetableId}")
    public ResponseEntity<TimetableResponse> updateTimetable(
            @PathVariable Long timetableId,
            @RequestBody TimetableUpdateRequest request
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
    public ResponseEntity<Void> deleteTimetable(@PathVariable Long timetableId) {
        timetableService.deleteTimetable(timetableId);
        return ResponseEntity.noContent().build();
    }

    // ================================
    // 🔥 AI 시간표 API (FE 스펙 일치)
    // ================================

    /** AI 시간표 생성 */
    @Operation(summary = "AI 시간표 생성")
    @PostMapping("/ai")
    public ResponseEntity<TimetableResponse> generateAiTimetable(
            @RequestBody AiTimetableRequest request
    ) {
        // userId는 request 안에 포함되어 있음 (프론트에서 body에 넣어서 보냄)
        TimetableResponse response = timetableService.generateAiTimetable(request);
        return ResponseEntity.ok(response);
    }

    /** AI 시간표 메타 저장 (resultSummary 저장) */
    @Operation(summary = "AI 시간표 메타 저장")
    @PutMapping("/ai")
    public ResponseEntity<AiTimetableResponse> saveAiTimetable(
            @RequestBody AiTimetableSaveRequest request
    ) {
        AiTimetableResponse response = aiTimetableService.saveAiTimetable(request);
        return ResponseEntity.ok(response);
    }

    /** AI 시간표 조회 */
    @Operation(summary = "AI 시간표 조회")
    @GetMapping("/ai")
    public ResponseEntity<AiTimetableResponse> getAiTimetable(
            @RequestParam Long userId
    ) {
        AiTimetableResponse response = aiTimetableService.getAiTimetable(userId);
        return ResponseEntity.ok(response);
    }

    /** AI 시간표 삭제 */
    @Operation(summary = "AI 시간표 삭제")
    @DeleteMapping("/ai")
    public ResponseEntity<Void> deleteAiTimetable(
            @RequestParam Long userId
    ) {
        aiTimetableService.deleteAiTimetable(userId);
        return ResponseEntity.noContent().build();
    }

    // ================================
    // 이미지 분석 (시간표 사진 → JSON)
    // ================================
    @Operation(
            summary = "이전 학기 시간표 사진 업로드",
            description = """
                에브리타임 등에서 캡쳐한 시간표 이미지를 업로드하면
                OpenAI가 이미지 내용을 분석해서
                강의명/요일/교시/강의실 리스트를 JSON으로 반환합니다.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "분석 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TimetableImageImportResponse.class)
            )
    )
    @PostMapping(
            value = "/import/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<TimetableImageImportResponse> importTimetableImage(
            @RequestPart("file") MultipartFile file
    ) {
        TimetableImageImportResponse response =
                timetableImageImportService.importFromImage(file);
        return ResponseEntity.ok(response);
    }
}