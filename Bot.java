import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

public class Bot {
    public static void main(String[] args) {
        // 환경 변수 가져오기
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String message = System.getenv("SLACK_WEBHOOK_MSG");
        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");

        // LLM 요청 JSON 문자열 생성
        String llmRequestBody = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" + message + "\" } ] } ] }";

        // LLM API 요청 설정
        HttpClient llmClient = HttpClient.newHttpClient();
        HttpRequest llmRequest = HttpRequest.newBuilder()
            .uri(URI.create(llmUrl + "?key=" + llmKey))  // API 키를 URL에 포함
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(llmRequestBody, StandardCharsets.UTF_8))
            .build();

        String llmResponseText = "LLM 응답 실패"; // 기본 메시지

        try {
            HttpResponse<String> llmResponse = llmClient.send(
                llmRequest, HttpResponse.BodyHandlers.ofString()
            );

            if (llmResponse.statusCode() == 200) {
                String responseBody = llmResponse.body();
                llmResponseText = extractTextFromLLMResponse(responseBody);
            } else {
                llmResponseText = "LLM 응답 실패: " + llmResponse.statusCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            llmResponseText = "LLM 요청 중 오류 발생";
        }

        // Slack 메시지 JSON 문자열 생성
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

    // ✅ LLM 응답에서 순수 텍스트만 추출하는 메서드 (JSON 파싱 없이)
    private static String extractTextFromLLMResponse(String responseBody) {
        // "text": "~~~" 부분에서 텍스트만 추출
        int textStart = responseBody.indexOf("\"text\":");
        if (textStart == -1) {
            return "LLM 응답에서 텍스트를 찾을 수 없음";
        }

        textStart += 8; // `"text":` 이후 시작 위치
        int textEnd = responseBody.indexOf("\"", textStart + 1);
        if (textEnd == -1) {
            return "LLM 응답 파싱 오류";
        }

        return responseBody.substring(textStart + 1, textEnd);
    }
}
