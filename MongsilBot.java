import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class MongsilBot {
    public static void main(String[] args) {
        // 환경 변수 가져오기
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");
        // ✅ 1. 랜덤 질문 생성 (위로 메시지 요청)
        String[] questions = {
            "뀨우뀨우 안녕! 몽실아, 오늘도 힘들게 하루를 보낸 집사님을 위해 300자 이내의 따뜻한 위로와 명언 메시지를 전해줄래?🐹💖",
            "뀨우뀨우 안녕! 몽실아, 누군가 오늘 힘든 하루를 보냈대. 위로가 될 수 있는 300자 이내의 다정한 메시지와 명언을 전해볼래?🐹💖",
            "뀨우뀨우 안녕! 몽실아, 집사님이 지쳤대. 네가 줄 수 있는 300자 이내의 가장 따뜻한 격려와 응원의 말과 명언을 전달해줄 수 있을까?🐹💖",
            "뀨우뀨우 안녕! 몽실아, 오늘 하루도 열심히 살아낸 집사님에게 너의 사랑스러운 말로 힘이 되는 300자 이내의 메시지와 명언을 전해줘!🐹💖",
            "뀨우뀨우 안녕! 몽실아, 때로는 힘든 날도 있는 법이지? 지금 힘들어하는 사람들에게 너만의 방식으로 용기와 명언을 300자 이내의 메시지로 줄 수 있을까?🐹💖",
            "뀨우뀨우 안녕! 몽실아, 세상은 가끔 힘들지만, 너의 귀여운 응원이 있다면 괜찮아질 것 같아. 위로와 용기의 메시지와 명언을 300자 이내로 보내줄래?🐹💖"
        };
        String question = questions[new Random().nextInt(questions.length)];

        System.out.println("📝 몽실봇 질문: " + question);

        // ✅ 2. Gemini API 요청
        String llmResponseText = getGeminiResponse(llmUrl, llmKey, question);
        String slackMessage = "🦙 *몽실봇*\n\n*질문:* " + question + "\n*답변:* " + llmResponseText;
        System.out.println("🤖 몽실이의 답변: " + llmResponseText);

        // ✅ 3. Gemini API 요청 (이미지 생성)
        String imageUrl = generateImage(llmUrl, llmKey, "A cute guinea pig named Mongsil comforting a tired person with a warm hug, soft colors, anime-style.");
        System.out.println("🖼️ 생성된 이미지 URL: " + imageUrl);

        // ✅ 4. Slack으로 메시지 전송 (이미지 포함)
        sendToSlack(webhookUrl, slackMessage, imageUrl);
    }

    // ✅ Gemini API 호출 함수 (텍스트 응답)
    private static String getGeminiResponse(String llmUrl, String llmKey, String question) {
        String requestBody = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" + question + "\" } ] } ] }";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(llmUrl + "?key=" + llmKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                return extractTextFromGeminiResponse(responseBody);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "몽실이가 답변을 못 찾았어요! 😢";
    }

   // ✅ Gemini API 호출 함수 (이미지 생성)
    private static String generateImage(String llmUrl, String llmKey, String prompt) {
        String requestBody = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" + prompt + "\" } ] } ] }";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(llmUrl + "?key=" + llmKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractImageUrlFromGeminiResponse(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "몽실이가 이미지를 생성하지 못했어요! 😢";
    }


    // ✅ Gemini 응답에서 텍스트 추출
    private static String extractTextFromGeminiResponse(String responseBody) {
        int textStart = responseBody.indexOf("\"text\":");
        if (textStart == -1) return "답변을 찾을 수 없음";

        textStart += 8;
        int textEnd = responseBody.indexOf("\"", textStart + 1);
        if (textEnd == -1) return "응답 파싱 오류";

        return responseBody.substring(textStart + 1, textEnd);
    }

    // ✅ Gemini 응답에서 이미지 URL 추출 (JSON 정교한 분석)
    private static String extractImageUrlFromGeminiResponse(String responseBody) {
        int urlStart = responseBody.indexOf("\"url\":\"");
        if (urlStart == -1) return "이미지를 찾을 수 없음";

        urlStart += 7; // "url":" 이후 시작 위치
        int urlEnd = responseBody.indexOf("\"", urlStart);
        if (urlEnd == -1) return "이미지 URL 파싱 오류";

        return responseBody.substring(urlStart, urlEnd);
    }

    // ✅ Slack 메시지 전송 함수 (이미지 포함)
    private static void sendToSlack(String webhookUrl, String message, String imageUrl) {
        String requestBody = "{"
            + "\"text\": \"" + message.replace("\"", "\\\"") + "\","
            + "\"attachments\": [{"
            + "\"text\": \"몽실이의 따뜻한 위로 메시지와 함께 이미지를 확인하세요!\","
            + "\"image_url\": \"" + imageUrl + "\","
            + "\"fallback\": \"이미지를 불러올 수 없습니다.\""
            + "}]"
            + "}";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // ✅ Slack 응답을 확인해서 오류 여부 확인
            System.out.println("📩 Slack API 응답: " + response.body());

            if (response.statusCode() == 200) {
                System.out.println("✅ Slack 메시지 전송 완료!");
            } else {
                System.out.println("❌ Slack 전송 실패: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
