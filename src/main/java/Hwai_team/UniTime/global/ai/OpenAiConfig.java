package Hwai_team.UniTime.global.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Bean
    public OpenAiClient openAiClient() {
        return new OpenAiClient(apiKey);
    }
}