import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.json.JSONObject;  // JSON 파싱을 위한 라이브러리

public class Bot {
    public static void main(String[] args) {
        // 환경 변수 가져오기
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String message = System.getenv("SLACK_WEBHOOK_MSG");

        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");

        // LLM 요청 JSON 생성
        JSONObject llmRequestBody = new JSONObject();
        llmRequestBody.put("contents", new JSONObject()
            .put("parts", new JSONObject().put("text", message)));

        // LLM API 요청 설정
        HttpClient llmClient = HttpClient.newHttpClient();
        HttpRequest llmRequest = HttpRequest.newBuilder()
            .uri(URI.create(llmUrl + "?key=" + llmKey))  // 인증키는 URL에 추가
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(llmRequestBody.toString(), StandardCharsets.UTF_8))
            .build();

        String llmResponseText = ""; // LLM 응답 메시지

        try {
            HttpResponse<String> llmResponse = llmClient.send(
                llmRequest, HttpResponse.BodyHandlers.ofString()
            );

            if (llmResponse.statusCode() == 200) {
                JSONObject responseJson = new JSONObject(llmResponse.body());
                llmResponseText = responseJson.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONArray("content")
                        .getJSONObject(0)
                        .getString("text");
            } else {
                llmResponseText = "LLM 응답 실패: " + llmResponse.statusCode();
            }

        } catch (Exception e) {
            e.printStackTrace();
            llmResponseText = "LLM 요청 중 오류 발생";
        }

        // Slack 메시지 JSON 생성
        JSONObject slackRequestBody = new JSONObject();
        slackRequestBody.put("text", llmResponseText);

        // Slack API 요청 설정
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(slackRequestBody.toString(), StandardCharsets.UTF_8))
            .build();

        try {
            HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString()
            );
            System.out.println("Slack 요청 코드: " + response.statusCode());
            System.out.println("Slack 응답 결과: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
