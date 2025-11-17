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

    /**
     * Ai 시간표 생성 컨트롤러
     *
     * @author 김민호
     * @param userId, message, year, semeseter
     * @return id, title, year, semester, items
     */
    @Operation(
            summary = "AI 시간표 생성 (DB 기반 자동 추천)",
            description = """
        사용자의 자연어 요청(message)과 유저 정보(학과/학년)를 기반으로
        데이터베이스에 저장된 강의 목록(courses)에서 조건에 맞는 과목들을 자동으로 선정하여
        시간표(Timetable)를 생성합니다.

        🔍 주요 동작
        - userId 를 이용해 유저 학과/학년 정보 조회
        - message(요약 텍스트)를 분석하여 조건 파악:
            · 재수강 과목 우선 포함
            · "주 N일" 등교 제한
            · "1교시 피하기" 여부
        - courses 테이블에서 유저 학년과 정확히 일치하는 추천학년(recommended_grade)의 전공을 우선 배치
        - 동일 강의 코드 / 동일 강의명 중복 자동 제거
        - 요일 및 시간 중복 자동 방지
        - 최종 생성된 시간표를 AI 대표 시간표(AiTimetable)로 자동 저장

        💡 message 필드
        - 이 값은 "챗봇과의 대화 내용"을 서버에서 요약해서 만든 한 줄짜리 조건 요약입니다.
        - 일반적인 흐름 예시:
          1) 채팅 화면에서 유저 대화를 백엔드에 저장
          2) 요약용 API (예: POST /api/ai/summary/timetable) 를 호출해서
             서버가 userId 기준으로 대화를 읽고, 시간표 조건을 요약한 문자열(summary)을 생성
          3) 그 summary 문자열을 AiTimetableRequest.message 에 그대로 넣어서
             POST /api/timetables/ai 를 호출해 시간표를 생성
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
                                      "message": "전자컴퓨터공학과 2학년, 주 3일 등교, 1교시 피하기, Advanced Calculus 2 재수강 필수",
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
                            schema = @Schema(implementation = TimetableResponse.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "id": 47,
                          "title": "김민호의 2학년 전자컴퓨터공학과 시간표",
                          "year": 2025,
                          "semester": 1,
                          "items": [
                            {
                              "id": 537,
                              "courseId": 537,
                              "courseName": "임베디드시스템",
                              "credit": 3,
                              "professor": "이광영",
                              "dayOfWeek": "TUE",
                              "startPeriod": 21,
                              "endPeriod": 22,
                              "room": "북-317",
                              "category": "전심",
                              "recommendedGrade": 2
                            }
                          ]
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "500", description = "시간표 생성 중 오류 발생")
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