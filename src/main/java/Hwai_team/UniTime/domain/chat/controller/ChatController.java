package Hwai_team.UniTime.domain.chat.controller;

import Hwai_team.UniTime.domain.chat.dto.ChatRequest;
import Hwai_team.UniTime.domain.chat.dto.ChatResponse;
import Hwai_team.UniTime.domain.chat.service.ChatService;
import Hwai_team.UniTime.domain.user.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "챗봇 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "챗봇에게 메시지 보내기", description = "유저 메시지를 보내면 GPT 기반 챗봇 답변을 반환합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "챗봇에게 메시지 보내기",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(
                            name = "쳇봇 테스트",
                            summary = "챗봇 테스트",
                            value = """
                                    {
                                      "userId": 1,
                                      "message": "여기에 대화 내용을 입력하세요",
                                      "conversationId": "1"
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
}