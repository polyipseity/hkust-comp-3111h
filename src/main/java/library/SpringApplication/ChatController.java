package library.SpringApplication;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import library.utils.Alerts;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/openai")
@RestController
public class ChatController {
    private final OpenAIClientBuilder openAIClientBuilder = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential("d7d5f6c01f3c493292402ec61cafe248"))
            .endpoint("https://hkust.azure-api.net");

    private final AzureOpenAiChatOptions openAIChatOptions = AzureOpenAiChatOptions.builder()
            .deploymentName("gpt-4o-mini")
            .build();

    private final AzureOpenAiChatModel chatModel = AzureOpenAiChatModel.builder()
            .openAIClientBuilder(openAIClientBuilder)
            .defaultOptions(openAIChatOptions)
            .build();

    @GetMapping
    public String getResponse(String input) {
        try {
            return chatModel.call(input);
        } catch (Exception e) {
            Alerts.showErrorDialog("Something went wrong with the AI generator, try again later.");
            return "";
        }
    }
}
