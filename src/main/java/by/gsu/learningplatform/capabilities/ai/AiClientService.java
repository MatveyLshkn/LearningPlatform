package by.gsu.learningplatform.capabilities.ai;

import by.gsu.learningplatform.core.config.LearningPlatformProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiClientService {

    private static final Logger log = LoggerFactory.getLogger(AiClientService.class);

    private final OllamaApi ollamaApi;
    private final LearningPlatformProperties learningPlatformProperties;

    public AiClientService(OllamaApi ollamaApi, LearningPlatformProperties learningPlatformProperties) {
        this.ollamaApi = ollamaApi;
        this.learningPlatformProperties = learningPlatformProperties;
    }

    public String generate(String systemPrompt, String userPrompt) {
        try {
            final var options = OllamaOptions.builder()
                    .temperature(learningPlatformProperties.ai().temperature())
                    .numPredict(learningPlatformProperties.ai().maxTokens())
                    .build();
            final var request = OllamaApi.ChatRequest.builder(learningPlatformProperties.ai().model())
                    .stream(false)
                    .think(false)
                    .options(options)
                    .messages(List.of(
                            OllamaApi.Message.builder(OllamaApi.Message.Role.SYSTEM).content(systemPrompt).build(),
                            OllamaApi.Message.builder(OllamaApi.Message.Role.USER).content(userPrompt).build()))
                    .build();
            final var response = ollamaApi.chat(request);
            if (response == null || response.message() == null) {
                throw new AiIntegrationException("AI provider returned empty content");
            }
            final var content = response.message().content();
            if (content == null || content.isBlank()) {
                final var thinking = response.message().thinking();
                if (thinking != null && !thinking.isBlank()) {
                    return thinking;
                }
                throw new AiIntegrationException("AI provider returned empty content");
            }
            return content;
        } catch (Exception ex) {
            log.warn("AI provider call failed: {}", ex.getMessage(), ex);
            throw new AiIntegrationException("AI provider call failed: " + ex.getMessage());
        }
    }
}
