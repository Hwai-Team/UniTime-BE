package Hwai_team.UniTime.global.ai;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

public class OpenAiClient {

    private final WebClient webClient;

    public OpenAiClient(String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * OpenAI ChatGPT API에 프롬프트를 보내고 결과(JSON string)를 받아온다.
     */
    public String askTimetableJson(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", new Object[]{
                        Map.of("role", "system", "content", "너는 대학 시간표 생성 전문가야."),
                        Map.of("role", "user", "content", prompt)
                },
                "response_format", Map.of("type", "json_object")
        );

        return webClient.post()
                .uri("/chat/completions")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    try {
                        Map<String, Object> choice =
                                ((java.util.List<Map<String, Object>>) response.get("choices")).get(0);
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        return (String) message.get("content");
                    } catch (Exception e) {
                        throw new RuntimeException("OpenAI 응답 파싱 실패: " + e.getMessage(), e);
                    }
                })
                // ❌ 가짜 JSON 돌려주지 말고
                .block();
    }

    public WebClient getWebClient() {
        return webClient;
    }
}