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
                      - startPeriod: 시작 교시 번호 (정수)
                      - endPeriod: 끝 교시 번호 (정수)
                      - room: 강의실 텍스트 전체 (없으면 null)

                    ⚠️ 교시 번호는 "시간대"에 따라 다음 규칙을 반드시 따라야 한다.
                    시간표의 실제 시작/종료 시간을 보고, 거기에 맞는 교시 번호를 골라라.

                    [일반 교시(50분짜리)]
                    - 1교시:  09:00 ~ 09:50  → startPeriod=1,  endPeriod=1
                    - 2교시:  10:00 ~ 10:50 → startPeriod=2,  endPeriod=2
                    - 3교시:  11:00 ~ 11:50 → startPeriod=3,  endPeriod=3
                    - 4교시:  12:00 ~ 12:50 → startPeriod=4,  endPeriod=4
                    - 5교시:  13:00 ~ 13:50 → startPeriod=5,  endPeriod=5
                    - 6교시:  14:00 ~ 14:50 → startPeriod=6,  endPeriod=6
                    - 7교시:  15:00 ~ 15:50 → startPeriod=7,  endPeriod=7
                    - 8교시:  16:00 ~ 16:50 → startPeriod=8,  endPeriod=8
                    - 9교시:  17:00 ~ 17:50 → startPeriod=9,  endPeriod=9

                    [블록 교시(1시간 15분짜리)]
                    - 21교시: 09:00 ~ 10:15 → startPeriod=21, endPeriod=21
                    - 22교시: 10:30 ~ 11:45 → startPeriod=22, endPeriod=22
                    - 23교시: 12:00 ~ 13:15 → startPeriod=23, endPeriod=23
                    - 24교시: 13:30 ~ 14:45 → startPeriod=24, endPeriod=24
                    - 25교시: 15:00 ~ 16:15 → startPeriod=25, endPeriod=25
                    - 26교시: 16:30 ~ 17:45 → startPeriod=26, endPeriod=26

                    규칙:
                    - 이미지에 시간이 09:00~10:15로 보이면, 이건 반드시 21교시로 간주하고 startPeriod=21, endPeriod=21 로 적어라.
                    - 이미지에 시간이 09:00~09:50로 보이면, 이건 1교시로 간주하고 startPeriod=1, endPeriod=1 로 적어라.
                    - 시간이 텍스트로만 "1교시", "2교시"라고 적혀 있어도,
                      가능하면 위의 시간표(09:00, 10:00, ...) 기준으로 교시 번호를 맞춰라.
                    - 2개 이상의 연속 교시(예: 1~2교시)로 보이면, 시작은 1, 끝은 2 같은 식으로 적어라.
                      (블록 교시도 마찬가지로 21~22 등으로 필요 시 확장 가능하지만,
                       기본적으로 하나의 블록(21, 22, ...)은 startPeriod=endPeriod 로 맞춰라.)

                    반드시 JSON 배열만 반환해.
                    예:
                    [
                      {
                        "courseName": "운영체제",
                        "courseCode": "CS301",
                        "dayOfWeek": "MON",
                        "startPeriod": 21,
                        "endPeriod": 21,
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