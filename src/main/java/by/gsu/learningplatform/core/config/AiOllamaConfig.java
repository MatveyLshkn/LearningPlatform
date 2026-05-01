package by.gsu.learningplatform.core.config;

import lombok.val;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.util.StringUtils;

@Configuration
public class AiOllamaConfig {

    @Bean
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
                               @Value("${spring.ai.ollama.api-key:}") String apiKey) {
        val builder = OllamaApi.builder().baseUrl(baseUrl);
        if (StringUtils.hasText(apiKey) && !"replace-with-your-ollama-api-key".equals(apiKey)) {
            builder.restClientBuilder(RestClient.builder().defaultHeader("Authorization", "Bearer " + apiKey));
            builder.webClientBuilder(WebClient.builder().defaultHeader("Authorization", "Bearer " + apiKey));
        }
        return builder.build();
    }

    @Bean
    public ChatModel chatModel(final OllamaApi ollamaApi, final LearningPlatformProperties properties) {
        val options = OllamaOptions.builder()
                .model(properties.ai().model())
                .temperature(properties.ai().temperature())
                .numPredict(properties.ai().maxTokens())
                .build();

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();
    }
}
