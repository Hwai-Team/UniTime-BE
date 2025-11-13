// src/main/java/Hwai_team/UniTime/domain/timetable/service/TimetableImageImportService.java
package Hwai_team.UniTime.domain.timetable.service;

import Hwai_team.UniTime.domain.timetable.dto.TimetableImageImportResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableImageImportService {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final ObjectMapper objectMapper;

    // 간단하게 new 로 하나 써도 됨
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api-key}")
    private String openAiApiKey;

    public TimetableImageImportResponse importFromImage(MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                throw new IllegalArgumentException("업로드된 이미지가 비어 있습니다.");
            }

            // 1) 이미지 base64 인코딩
            String base64 = Base64.getEncoder().encodeToString(image.getBytes());

            // 2) 프롬프트
            String systemPrompt = """
                    너는 대학 시간표 이미지(에브리타임 캡쳐)를 분석하는 도우미야.
                    이미지에 있는 각 강의에 대해 아래 필드를 뽑아서 JSON 배열로만 답해.
                    필드:
                      - courseName: 강의명 (string)
                      - courseCode: 학수번호가 보이면 적고, 없으면 null
                      - dayOfWeek: MON/TUE/WED/THU/FRI/SAT 중 하나
                      - startPeriod: 시작 교시 (정수, 1~25 같은 형태)
                      - endPeriod: 끝 교시 (정수)
                      - room: 강의실 텍스트 전체 (없으면 null)

                    반드시 JSON 배열만 반환해. 예:
                    [
                      {
                        "courseName": "운영체제",
                        "courseCode": "CS301",
                        "dayOfWeek": "MON",
                        "startPeriod": 21,
                        "endPeriod": 22,
                        "room": "IT-401"
                      }
                    ]
                    """;

            // 3) OpenAI Vision 호출 → JSON 문자열
            String json = callOpenAiVision(systemPrompt, base64);

            // 4) JSON → DTO 리스트
            List<TimetableImageImportResponse.Item> items =
                    objectMapper.readValue(
                            json,
                            new TypeReference<List<TimetableImageImportResponse.Item>>() {}
                    );

            return TimetableImageImportResponse.builder()
                    .items(items)
                    .build();

        } catch (Exception e) {
            // 로그 남기고 싶으면 logger 써서 찍어
            throw new RuntimeException("시간표 이미지 분석에 실패했습니다.", e);
        }
    }

    /**
     * OpenAI Chat Completions(vision 지원 모델) 호출
     */
    private String callOpenAiVision(String systemPrompt, String base64Image) throws Exception {
        // 1) 요청 바디 JSON 구성
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", "gpt-4.1-mini");   // vision 되는 모델

        ArrayNode messages = root.putArray("messages");

        // system 메시지
        ObjectNode sysMsg = messages.addObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);

        // user 메시지 (텍스트 + 이미지)
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        ArrayNode content = userMsg.putArray("content");

        // 텍스트 파트
        ObjectNode textPart = content.addObject();
        textPart.put("type", "text");
        textPart.put("text", "이 시간표 이미지를 분석해서 위에서 정의한 JSON 배열만 반환해.");

        // 이미지 파트 (base64 data URL)
        ObjectNode imagePart = content.addObject();
        imagePart.put("type", "image_url");
        ObjectNode imageUrl = imagePart.putObject("image_url");
        imageUrl.put("url", "data:image/png;base64," + base64Image);

        String requestBody = objectMapper.writeValueAsString(root);

        // 2) 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 3) HTTP 호출
        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity(OPENAI_CHAT_URL, entity, JsonNode.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("OpenAI 호출 실패: " + response.getStatusCode());
        }

        JsonNode body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("OpenAI 응답 body가 비어 있음");
        }

        JsonNode choices = body.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("OpenAI 응답에 choices가 없음: " + body.toString());
        }

        JsonNode message = choices.get(0).get("message");
        JsonNode contentNode = message.get("content");

        if (contentNode != null && contentNode.isTextual()) {
            // 프롬프트에서 JSON 배열만 달라고 했으니 이걸 그대로 파싱에 사용
            return contentNode.asText();
        }

        // 구조 예상이랑 다르면 그대로 덤프해서 에러
        throw new IllegalStateException("예상치 못한 OpenAI 응답 구조: " + message.toString());
    }
}