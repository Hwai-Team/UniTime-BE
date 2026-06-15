// src/main/java/Hwai_team/UniTime/global/ai/OpenAiClient.java
package Hwai_team.UniTime.global.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private final WebClient webClient;

    // ✅ application.yml 의 openai.api-key 사용
    public OpenAiClient(@Value("${openai.api-key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * ✅ 일반 챗봇 대화용 - 텍스트 답변
     *  - ChatService 에서 사용
     */
    public String askChat(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", new Object[]{
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                }
        );

        return callChatCompletions(requestBody, "챗봇");
    }

    /**
     * ✅ 시간표 이미지(Vision) 분석용 - JSON 문자열 응답
     *  - TimetableImageImportService 에서 사용
     */
    public String askVisionJson(String systemPrompt, String userText, String base64Image) {
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4.1-mini", // vision 지원 모델
                "messages", new Object[]{
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", new Object[]{
                                Map.of("type", "text", "text", userText),
                                Map.of("type", "image_url", "image_url",
                                        Map.of("url", "data:image/png;base64," + base64Image))
                        })
                }
        );

        return callChatCompletions(requestBody, "시간표 이미지");
    }

    private String callChatCompletions(Map<String, Object> requestBody, String context) {
        return webClient.post()
                .uri("/chat/completions")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    try {
                        List<Map<String, Object>> choices =
                                (List<Map<String, Object>>) response.get("choices");
                        Map<String, Object> choice = choices.get(0);
                        Map<String, Object> message =
                                (Map<String, Object>) choice.get("message");
                        return (String) message.get("content");
                    } catch (Exception e) {
                        throw new RuntimeException("OpenAI " + context + " 응답 파싱 실패: " + e.getMessage(), e);
                    }
                })
                .block(); // 동기 방식으로 사용
    }
}