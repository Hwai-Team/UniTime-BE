package Hwai_team.UniTime.domain.timetable.controller;

import Hwai_team.UniTime.domain.timetable.dto.TimetableSummaryResponse;
import Hwai_team.UniTime.domain.timetable.service.AiTimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/summary")
@RequiredArgsConstructor
@Tag(
        name = "AI Summary",
        description = "AI 관련 요약 정보 조회 API"
)
public class AiSummaryController {

    private final AiTimetableService aiTimetableService;

    @Operation(
            summary = "AI 시간표 생성 조건 요약 조회",
            description = """
                    userId 기준으로, 최근 AI 시간표 생성에 사용된 요약 메세지(prompt)를 조회합니다.
                    
                    - 내부적으로 AiTimetable.prompt 값을 사용합니다.
                    - 아직 AI 시간표가 한 번도 생성되지 않았다면 404 에러를 반환합니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TimetableSummaryResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "userId": 1,
                                      "summary": "전자컴퓨터공학과 2학년, 주 3일 등교, 1교시 피하고 Advanced Calculus2 재수강 포함"
                                    }
                                    """
                    )
            )
    )
    @GetMapping("/timetable")
    public ResponseEntity<TimetableSummaryResponse> getTimetableSummary(
            @Parameter(description = "요약을 조회할 유저 ID", example = "1")
            @RequestParam Long userId
    ) {
        TimetableSummaryResponse res = aiTimetableService.getTimetableSummary(userId);
        return ResponseEntity.ok(res);
    }
}