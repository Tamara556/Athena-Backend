package com.athena.llm.config;

import com.athena.llm.ChatProvider;
import com.athena.llm.EmbeddingProvider;
import com.athena.llm.spi.lmstudio.LmStudioChatProvider;
import com.athena.llm.spi.lmstudio.LmStudioEmbeddingProvider;
import io.netty.channel.ChannelOption;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@EnableConfigurationProperties(LlmProviderProperties.class)
public class LlmAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "llmChatWebClient")
    public WebClient llmChatWebClient(LlmProviderProperties properties) {
        return webClient(properties.chat().baseUrl(), properties.chat().apiKey(),
                (int) properties.chat().connectTimeout().toMillis(), properties.chat().readTimeout());
    }

    @Bean
    @ConditionalOnMissingBean(name = "llmEmbeddingWebClient")
    public WebClient llmEmbeddingWebClient(LlmProviderProperties properties) {
        return webClient(properties.embedding().baseUrl(), properties.embedding().apiKey(),
                (int) properties.embedding().connectTimeout().toMillis(), properties.embedding().readTimeout());
    }

    @Bean
    @ConditionalOnMissingBean(ChatProvider.class)
    public ChatProvider chatProvider(WebClient llmChatWebClient, LlmProviderProperties properties) {
        return new LmStudioChatProvider(llmChatWebClient, properties.chat());
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingProvider.class)
    public EmbeddingProvider embeddingProvider(WebClient llmEmbeddingWebClient, LlmProviderProperties properties) {
        return new LmStudioEmbeddingProvider(llmEmbeddingWebClient, properties.embedding());
    }

    private WebClient webClient(String baseUrl, String apiKey, int connectTimeoutMillis, java.time.Duration readTimeout) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .responseTimeout(readTimeout);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(headers -> {
                    if (apiKey != null && !apiKey.isBlank()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .build();
    }
}
