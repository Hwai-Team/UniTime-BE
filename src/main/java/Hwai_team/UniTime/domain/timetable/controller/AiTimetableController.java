package Hwai_team.UniTime.domain.timetable.controller;

import Hwai_team.UniTime.domain.timetable.dto.AiTimetableRequest;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableResponse;
import Hwai_team.UniTime.domain.timetable.dto.AiTimetableSaveRequest;
import Hwai_team.UniTime.domain.timetable.dto.TimetableResponse;
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
@RequestMapping("/api/timetables/ai")
@RequiredArgsConstructor
@Tag(
        name = "AI Timetable",
        description = "AI 기반 시간표 생성 / AI 대표 시간표 조회·수정·삭제 API"
)
public class AiTimetableController {

    private final AiTimetableService aiTimetableService;

    // =======================
    // 1) AI 시간표 생성
    // =======================
    @Operation(
            summary = "AI 시간표 생성 (Timetable + AiTimetable 자동 저장)",
            description = """
                사용자의 자연어 요청과 유저 정보(학과/학년)를 기반으로,
                DB에 저장된 강의 목록(courses) 중에서 AI가 과목을 선택해 시간표를 자동 생성합니다.
                
                동작 요약:
                - userId로 유저를 조회해 학과/학년 정보를 가져옵니다.
                - 해당 학과/추천 학년에 맞는 강의들을 courses 테이블에서 조회합니다.
                - 조회한 강의 목록 + 사용자의 요구사항(message)을 프롬프트에 넣어
                  OpenAI로부터 JSON 형식의 시간표(TimetablePlan)를 응답받습니다.
                - 응답에 포함된 courseCode를 기준으로 실제 Course 정보를 매핑하여
                  Timetable / TimetableItem 엔티티를 생성·저장합니다.
                - 동시에, 생성된 Timetable을 해당 유저의 AI 대표 시간표(AiTimetable)로 자동 저장/갱신합니다.
                  (유저당 AiTimetable은 1개만 유지)
                
                예시 시나리오:
                - "전자컴퓨터공학과 2학년 시간표 만들어줘.
                   통학이 멀어서 주 3일만 학교 가고 싶고,
                   1교시는 피했으면 좋겠어.
                   Advanced Calculus2는 꼭 재수강해야 해."
                """
            ,
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
                                              "message": "전자컴퓨터공학과 2학년 시간표 만들어줘. 통학이 멀어서 주 3일만 학교 가고 싶고 1교시는 피했으면 좋겠어. Advanced Calculus2는 꼭 재수강해야 해.",
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
                    description = "시간표 생성 성공 (Timetable / AiTimetable 모두 저장 완료)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TimetableResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 47,
                                              "title": "2025-1학기 전자컴퓨터공학과 2학년 추천 시간표",
                                              "year": 2025,
                                              "semester": 1,
                                              "items": [
                                                {
                                                  "courseName": "Advanced Calculus2",
                                                  "dayOfWeek": "TUE",
                                                  "startPeriod": 2,
                                                  "endPeriod": 3,
                                                  "room": "북-508",
                                                  "category": "전필"
                                                },
                                                {
                                                  "courseName": "자료구조",
                                                  "dayOfWeek": "WED",
                                                  "startPeriod": 3,
                                                  "endPeriod": 4,
                                                  "room": "공학관 101",
                                                  "category": "전선"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 데이터"),
            @ApiResponse(responseCode = "500", description = "AI 처리 또는 시간표 생성 중 오류 발생")
    })
    @PostMapping
    public ResponseEntity<TimetableResponse> createByAi(
            @org.springframework.web.bind.annotation.RequestBody AiTimetableRequest request
    ) {
        Timetable timetable = aiTimetableService.createByAi(request);
        return ResponseEntity.ok(TimetableResponse.from(timetable));
    }

    // =======================
    // 2) AI 시간표 수동 저장/수정
    // =======================
    @Operation(
            summary = "AI 대표 시간표 수동 저장/수정 (선택 기능)",
            description = """
                    이미 생성된 시간표(Timetable)를 이 유저의 'AI 대표 시간표'로 다시 지정하거나,
                    설명(resultSummary)을 수정하고 싶을 때 사용하는 선택적인 API입니다.
                    
                    기본 흐름:
                    - 일반적인 경우: POST /api/timetables/ai 를 호출하면
                      Timetable + AiTimetable 이 한 번에 자동으로 저장되므로,
                      별도의 호출이 필요 없습니다.
                    
                    - 이 API는 다음처럼 사용합니다.
                      1) 다른 API나 화면에서 임의의 시간표(Timetable)를 수정·생성한다.
                      2) 해당 시간표의 id를 timetableId로 넘겨
                         "이 시간표를 내 AI 대표 시간표로 쓰겠다" 라고 지정하거나
                         resultSummary 를 갱신한다.
                    """
            ,
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AiTimetableSaveRequest.class),
                            examples = @ExampleObject(
                                    name = "AI 시간표 수동 저장 예시",
                                    value = """
                                            {
                                              "userId": 1,
                                              "timetableId": 47,
                                              "resultSummary": "통학이 멀어서 주 3일만 등교하고, 1교시는 피한 전자컴퓨터공학과 2학년 추천 시간표입니다."
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "AI 대표 시간표 저장/갱신 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AiTimetableResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": 3,
                                              "userId": 1,
                                              "timetableId": 47,
                                              "title": "2025-1학기 전자컴퓨터공학과 2학년 추천 시간표",
                                              "resultSummary": "통학이 멀어서 주 3일만 등교하고, 1교시는 피한 전자컴퓨터공학과 2학년 추천 시간표입니다.",
                                              "createdAt": "2025-11-11T12:34:56"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @ApiResponse(responseCode = "404", description = "유저 또는 시간표를 찾을 수 없음")
    })
    @PutMapping
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
            summary = "AI 대표 시간표 메타 정보 조회",
            description = """
                    특정 유저가 저장한 AI 대표 시간표(AiTimetable)를 조회합니다.
                    
                    - 유저당 AiTimetable은 1개만 유지합니다.
                    - 응답으로 돌아오는 timetableId 를 이용해,
                      일반 시간표 조회 API로 실제 시간표 상세(Timetable + TimetableItem)를 조회할 수 있습니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AiTimetableResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "id": 3,
                                      "userId": 1,
                                      "timetableId": 47,
                                      "title": "2025-1학기 전자컴퓨터공학과 2학년 추천 시간표",
                                      "resultSummary": "통학이 멀고 1교시를 피하고 싶은 사용자를 위한 주 3일 등교 시간표입니다.",
                                      "createdAt": "2025-11-11T12:34:56"
                                    }
                                    """
                    )
            )
    )
    @GetMapping
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
            summary = "AI 대표 시간표 삭제",
            description = """
                    특정 유저가 저장한 AI 대표 시간표(AiTimetable) 기록을 삭제합니다.
                    
                    주의:
                    - Timetable / TimetableItem 자체를 삭제하는 것이 아니라,
                      "이 시간표를 AI 추천 시간표로 사용한다"는 연결(AiTimetable)만 제거합니다.
                    """
    )
    @ApiResponse(responseCode = "204", description = "삭제 성공 (응답 본문 없음)")
    @DeleteMapping
    public ResponseEntity<Void> deleteAiTimetable(
            @Parameter(description = "삭제할 유저 ID", example = "1")
            @RequestParam Long userId
    ) {
        aiTimetableService.deleteAiTimetable(userId);
        return ResponseEntity.noContent().build();
    }
}