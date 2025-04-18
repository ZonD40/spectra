package spectra.ru.notify.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import spectra.ru.notify.dto.UserResponseDto;

import java.util.ArrayList;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class UserService {

    WebClient webClient;

    public Mono<List<String>> getUserEmailListByIdListAsync(Iterable<Long> idList) {

        return webClient.post()
                .uri("/api/users/internal/batchEmail")
                .bodyValue(idList)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .map(ArrayList::new);
    }

    public Mono<UserResponseDto> getUserNameByIdAsync(Long userId) {
        return webClient.get()
                .uri(String.format("/api/users/%s", userId))
                .retrieve()
                .bodyToMono(UserResponseDto.class);
    }

}