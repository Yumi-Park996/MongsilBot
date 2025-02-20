import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;

public class Bot {
    public static void main(String[] args) {
        // 환경 변수 가져오기
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String message = System.getenv("SLACK_WEBHOOK_MSG");
        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");

        // LLM 요청 JSON 생성
        String llmRequestBody = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" + message + "\" } ] } ] }";

        // LLM API 요청 설정
        HttpClient llmClient = HttpClient.newHttpClient();
        HttpRequest llmRequest = HttpRequest.newBuilder()
            .uri(URI.create(llmUrl + "?key=" + llmKey))  // 인증키 추가
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(llmRequestBody, StandardCharsets.UTF_8))
            .build();

        String llmResponseText = ""; // LLM 응답 메시지

        try {
            HttpResponse<String> llmResponse = llmClient.send(
                llmRequest, HttpResponse.BodyHandlers.ofString()
            );

            if (llmResponse.statusCode() == 200) {
                // JSON 파싱하여 필요한 부분만 추출
                JSONObject responseJson = new JSONObject(llmResponse.body());
                JSONArray candidates = responseJson.getJSONArray("candidates");

                if (candidates.length() > 0) {
                    JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");

                    if (parts.length() > 0) {
                        llmResponseText = parts.getJSONObject(0).getString("text");
                    }
                }
            } else {
                llmResponseText = "LLM 응답 실패: " + llmResponse.statusCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            llmResponseText = "LLM 요청 중 오류 발생";
        }

        // Slack 메시지 JSON 생성 (필요한 응답 텍스트만 보냄)
        String slackRequestBody = "{ \"text\": \"" + llmResponseText.replace("\"", "\\\"") + "\" }";

        // Slack API 요청 설정
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(slackRequestBody, StandardCharsets.UTF_8))
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
