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
     * ✅ AI 시간표 생성용 - JSON만 응답 받는 메서드
     *  - AiTimetableService 에서 사용
     */
    public String askTimetableJson(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", new Object[]{
                        Map.of("role", "system", "content", "너는 대학 시간표 생성 전문가야."),
                        Map.of("role", "user", "content", prompt)
                },
                // JSON 객체 형태로만 응답하도록 강제
                "response_format", Map.of("type", "json_object")
        );

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
                        return (String) message.get("content"); // <- JSON 문자열
                    } catch (Exception e) {
                        throw new RuntimeException("OpenAI 시간표 응답 파싱 실패: " + e.getMessage(), e);
                    }
                })
                .block(); // 동기 방식으로 사용
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
                        return (String) message.get("content"); // <- 챗봇 답변 텍스트
                    } catch (Exception e) {
                        throw new RuntimeException("OpenAI 챗봇 응답 파싱 실패: " + e.getMessage(), e);
                    }
                })
                .block();
    }

    public WebClient getWebClient() {
        return webClient;
    }
}