package spectra.ru.notify.config;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserWebClientConfig {

    @Value("${api.user-service.url}")
    String baseUrl;

    @Bean
    public WebClient userWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
