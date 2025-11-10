package Hwai_team.UniTime.domain.chat.controller;

import Hwai_team.UniTime.domain.chat.dto.ChatHistoryResponse;
import Hwai_team.UniTime.domain.chat.dto.ChatRequest;
import Hwai_team.UniTime.domain.chat.dto.ChatResponse;
import Hwai_team.UniTime.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat", description = "GPT 기반 챗봇 관련 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "챗봇에게 메시지 보내기",
            description = """
                    유저가 입력한 메시지를 GPT 기반 챗봇에게 전달하고,
                    챗봇의 답변을 반환합니다.
                    
                    서버는 userId 기준으로 대화 세션을 관리하므로
                    요청 바디의 conversationId는 보내지 않아도 되며,
                    보내더라도 서버에서 무시됩니다.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "응답 성공",
                            content = @Content(schema = @Schema(implementation = ChatResponse.class))),
                    @ApiResponse(responseCode = "400", description = "요청값 오류",
                            content = @Content(examples = @ExampleObject(value = "{\"error\": \"userId는 필수입니다.\"}"))),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "챗봇 요청 예시 (conversationId는 생략 가능)",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ChatRequest.class),
                    examples = @ExampleObject(
                            name = "Chat 예시",
                            summary = "챗봇 테스트 요청",
                            value = """
                                    {
                                      "userId": 1,
                                      "message": "내 시간표 알려줘"
                                    }
                                    """
                    )
            )
    )
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "유저의 전체 대화 기록 조회",
            description = "특정 유저의 모든 챗봇 대화 내역을 시간 순서대로 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = ChatHistoryResponse.class))),
                    @ApiResponse(responseCode = "404", description = "해당 유저의 대화 내역이 없음")
            }
    )
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<ChatHistoryResponse>> getHistory(
            @PathVariable Long userId
    ) {
        List<ChatHistoryResponse> history = chatService.getChatHistory(userId);
        return ResponseEntity.ok(history);
    }

    @Operation(
            summary = "유저의 대화 내역 전체 삭제",
            description = """
                    특정 유저의 모든 챗봇 대화 기록을 삭제합니다.
                    일반적으로 '대화 초기화' 기능에 사용됩니다.
                    """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공"),
                    @ApiResponse(responseCode = "404", description = "해당 유저의 대화 내역이 없음")
            }
    )
    @DeleteMapping("/history/{userId}")
    public ResponseEntity<Void> deleteHistory(
            @PathVariable Long userId
    ) {
        chatService.deleteChatHistory(userId);
        return ResponseEntity.noContent().build();
    }

}