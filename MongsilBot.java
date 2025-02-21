import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MongsilBot {
    public static void main(String[] args) {
        // 환경 변수 가져오기
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        String llmUrl = System.getenv("LLM_URL");
        String llmKey = System.getenv("LLM_KEY");

        // ✅ 1. 랜덤 질문 생성 (위로 메시지 요청)
        String[] questions = {
            "뀨우뀨우 안녕! 몽실아, 오늘도 힘들게 하루를 보낸 집사님을 위해 350자 이내의 따뜻한 위로와 조언 메시지를 전해줄래? 이모티콘을 담아서!🐹💖",
            "뀨우뀨우 안녕! 몽실아, 누군가 오늘 힘든 하루를 보냈대. 위로가 될 수 있는 350자 이내의 다정한 메시지와 조언을 전해볼래? 이모티콘을 담아서!🐹💖",
            "뀨우뀨우 안녕! 몽실아, 집사님이 지쳤대. 네가 줄 수 있는 350자 이내의 가장 따뜻한 격려와 응원의 말과 조언을 전달해줄 수 있을까? 이모티콘을 담아서!🐹💖",
            "뀨우뀨우 안녕! 몽실아, 오늘 하루도 열심히 살아낸 집사님에게 너의 사랑스러운 말로 힘이 되는 350자 이내의 메시지와 조언을 전해줘! 이모티콘을 담아서!🐹💖",
            "뀨우뀨우 안녕! 몽실아, 때로는 힘든 날도 있는 법이지? 지금 힘들어하는 사람들에게 너만의 방식으로 용기와 조언을 350자 이내의 메시지로 줄 수 있을까? 이모티콘을 담아서!🐹💖",
            "뀨우뀨우 안녕! 몽실아, 세상은 가끔 힘들지만, 너의 귀여운 응원이 있다면 괜찮아질 것 같아. 위로와 용기의 메시지와 조언을 350자 이내로 보내줄래? 이모티콘을 담아서!🐹💖"
        };
        String question = questions[new Random().nextInt(questions.length)];
        System.out.println("📝 몽실봇 질문: " + question);

        // ✅ 2. Gemini API 요청
        String llmResponseText = getGeminiResponse(llmUrl, llmKey, question);
        System.out.println("🤖 몽실이의 답변: " + llmResponseText);
        
        // LLM을 사용해 이미지 생성
        String image_url = getTogetherResponse("Create an animated-style illustration with a warm and bright atmosphere, depicting a cute guinea pig and a human together. The guinea pig has large, sparkling eyes and soft fur, making it an adorable character with a playful and lively expression. The human appears friendly and warm, interacting with the guinea pig by gently petting it or playing together.
The background should be a cozy indoor living room or a sunlit park, with bright and soft pastel tones. The animation style should resemble The Secret Life of Pets, capturing a similar aesthetic.");
        System.out.println("image url = " + image_url);

        // GitHub Issue 생성

        // ✅ 3. Slack으로 메시지 전송
        String slackMessage = "🦙 *몽실봇*\n\n*질문:* " + question + "\n*답변:* " + llmResponseText;
        sendToSlack(webhookUrl, slackMessage, image_url);
    }

    public static String getTogetherResponse(String prompt) {
        return callLLMApi2(prompt);
    }

    // ✅ Gemini API 호출 함수
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

    // ✅ Gemini 응답에서 텍스트 추출
    private static String extractTextFromGeminiResponse(String responseBody) {
        int textStart = responseBody.indexOf("\"text\":");
        if (textStart == -1) return "답변을 찾을 수 없음";

        textStart += 8;
        int textEnd = responseBody.indexOf("\"", textStart + 1);
        if (textEnd == -1) return "응답 파싱 오류";

        return responseBody.substring(textStart + 1, textEnd);
    }

     public static String callLLMApi2(String prompt) {
        String apiUrl = System.getenv("LLM2_API_URL");
        String apiKey = System.getenv("LLM2_API_KEY");
        String model = System.getenv("LLM2_MODEL");

        String payload = """
                {
                "prompt": "%s",
                "model": "%s",
                "width": 640,
                "height": 640,
                "steps": 4,
                "n": 1
                
                }
                """.formatted(prompt, model);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());

            if (response.statusCode() == 200) {
                return  extractImageUrl(response.body());
            } else {
                return "LLM API 오류: " + response.statusCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "예외 발생: " + e.getMessage();
        }
    }

    public static String extractImageUrl(String json) {
        // 정규식 패턴: "url": "이미지 URL"
        Pattern pattern = Pattern.compile("\"url\"\\s*:\\s*\"(https?://[^\"]+)\"");
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1); // 첫 번째 URL 반환
        }
        return "응답에서 이미지 URL을 찾을 수 없음";
    }

    // ✅ Slack 메시지 전송 함수
    private static void sendToSlack(String webhookUrl, String message, String imageUrl) {
        // JSON 문자열을 직접 생성 (이스케이프 처리 포함)
        String requestBody = "{ \"text\": \"" + message
                .replace("\\", "\\\\")  // 역슬래시 이스케이프
                .replace("\"", "\\\"")  // 큰따옴표 이스케이프
                .replace("\n", "\\n")   // 줄바꿈 이스케이프
                + "\", "
                + "\"attachments\": [{ \"image_url\": \"" + imageUrl + "\" }] }";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Slack 메시지 전송 완료!");
            } else {
                System.out.println("❌ Slack 전송 실패: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}