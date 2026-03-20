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

    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    public String analyzeMission(MultipartFile file, String missionTopic) throws IOException {
        RestTemplate restTemplate = new RestTemplate();

        // 1. 팀원 프롬프트: 엄격한 채점과 JSON 출력 강제
        String promptText = String.format(
                "You are a strict photography judge. Evaluate the image concept: \"%s\". " +
                        "Scoring: 0-100. If score >= 70 then PASS, else FAIL. " +
                        "Explain in Korean (1-2 sentences). " +
                        "Output JSON only: {\"concept\": \"%s\", \"score\": 0, \"reason\": \"...\", \"result\": \"PASS or FAIL\"}",
                missionTopic, missionTopic
        );

        // 2. 이미지 Base64 인코딩
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

        // 3. 구글 API 규격에 맞는 요청 바디 구성 (JSON)
        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mime_type", file.getContentType());
        inlineData.put("data", base64Image);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("inline_data", inlineData);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", promptText);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Arrays.asList(textPart, imagePart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_API_URL + apiKey, entity, String.class);

            // 5. 복잡한 구글 응답 껍데기에서 우리가 원하는 JSON 데이터만 추출
            JSONObject fullResponse = new JSONObject(response.getBody());
            String rawResult = fullResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            // 마크다운 기호(```json)가 섞여 나올 수 있으므로 정제해서 반환
            return rawResult.replaceAll("```json|```", "").trim();

        } catch (HttpClientErrorException e) {
            System.err.println("🚨 구글 API 에러: " + e.getResponseBodyAsString());
            return "API 호출 실패: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return "서버 내부 오류: " + e.getMessage();
        }
    }
}