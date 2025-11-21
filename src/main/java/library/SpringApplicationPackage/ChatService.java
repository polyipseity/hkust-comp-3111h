package library.SpringApplicationPackage;

import library.utils.Alerts;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

public class ChatService {
    private final String API_URL = "https://hkust.azure-api.net/openai/deployments/gpt-4o-mini/chat/completions?api-version=2024-10-21";
    private final String API_KEY = "d7d5f6c01f3c493292402ec61cafe248";

    public String getResponse(String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", API_KEY);

        // Build the request body matching your POST example
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content", "You are a book abstract reader."
        ));
        messages.add(Map.of(
                "role", "user",
                "content", prompt
        ));

        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // Extract and parse the AI response
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    if (message != null && message.containsKey("content")) {
                        return (String) message.get("content");
                    }
                }
            }

            return "Error: No AI response content found in the response";
        } catch (Exception e) {
            Alerts.showErrorDialog("Error: Could not get AI response at the moment");
            return "";
        }
    }
}
