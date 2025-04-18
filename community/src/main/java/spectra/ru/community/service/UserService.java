package spectra.ru.community.service;

import spectra.ru.community.api.dto.internal.UserInternalResponseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class UserService {

    WebClient webClient;

    public Mono<Set<UserInternalResponseDto>> getUserListByIdListAsync(Iterable<Long> idList) {

        return webClient.post()
                .uri("/api/users/internal/batch")
                .bodyValue(idList)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Set<UserInternalResponseDto>>() {})
                .map(HashSet::new);
    }

}
