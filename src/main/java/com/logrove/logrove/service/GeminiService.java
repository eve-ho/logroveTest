package com.logrove.logrove.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    // [2026 최신 주소] 404 에러를 방지하는 검증된 엔드포인트
    private final String PRO_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=";
    private final String LITE_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=";

    /**
     * [업그레이드] 이미지 태그 추출 (제한된 리스트 내에서 선택)
     */
    public String extractImageTags(MultipartFile file) throws IOException {
        String promptText =
                "You are an image tagging system.\n\n" +
                        "Select 3~5 tags that best describe the image.\n" +
                        "You MUST choose ONLY from the list below. Do NOT create new tags.\n\n" +
                        "[tag list]\n" +
                        "subject: person, man, woman, child, group, dog, cat, bird, car, building, street, food, flower, tree, mountain, ocean, sky\n" +
                        "genre: portrait, street, landscape, night, studio, indoor, outdoor, wildlife, macro, travel\n" +
                        "composition: centered, rule of thirds, symmetry, leading lines, depth, framing, minimalism, close-up, wide shot, low angle, high angle, eye level, shallow depth, background blur, silhouette, backlighting\n\n" +
                        "Rules:\n" +
                        "1. Only choose from the list.\n" +
                        "2. Only include clearly visible elements.\n" +
                        "3. No duplicates, no explanation.\n" +
                        "4. Output in Korean (Translate the selected English keywords to Korean).\n\n" +
                        "Output JSON only:\n" +
                        "{\"tags\":[\"태그1\",\"태그2\",\"태그3\"]}";

        return callGeminiApi(file, LITE_API_URL, promptText);
    }

    /**
     * 사진 미션 채점 (3.1 Pro 사용)
     */
    public String analyzeMission(MultipartFile file, String missionTopic) throws IOException {
        int threshold = 70;
        String promptText = String.format(
                "You are a strict photography judge. Evaluate the image concept: \"%s\"\n" +
                        "Scoring: 0-100. If score >= %d then PASS, else FAIL.\n" +
                        "Explain in Korean (1-2 sentences).\n" +
                        "Output JSON only: {\"concept\": \"%s\", \"score\": 0, \"reason\": \"...\", \"result\": \"PASS or FAIL\"}",
                missionTopic, threshold, missionTopic
        );

        return callGeminiApi(file, PRO_API_URL, promptText);
    }

    /**
     * 공통 API 호출 로직
     */
    private String callGeminiApi(MultipartFile file, String apiUrl, String promptText) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mime_type", file.getContentType());
        inlineData.put("data", base64Image);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", promptText);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("inline_data", inlineData);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Arrays.asList(textPart, imagePart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json");
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl + apiKey, new HttpEntity<>(requestBody, headers), String.class);
            JSONObject fullResponse = new JSONObject(response.getBody());
            String rawResult = fullResponse.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
            return rawResult.replaceAll("```json|```", "").trim();
        } catch (Exception e) {
            return "API 에러: " + e.getMessage();
        }
    }
}