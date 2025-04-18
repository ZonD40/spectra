package spectra.ru.community.api.controller;

import spectra.ru.community.api.dto.AnswerDto;
import spectra.ru.community.api.dto.internal.UserInternalResponseDto;
import spectra.ru.community.api.dto.spector.SpectorCreateUpdateDto;
import spectra.ru.community.api.dto.spector.SpectorResponseDto;
import spectra.ru.community.service.SpectorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "api/spectors")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpectorController {

    SpectorService spectorService;

    @PostMapping
    public SpectorResponseDto create(
            @RequestBody SpectorCreateUpdateDto spectorCreateUpdateDto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return spectorService.create(spectorCreateUpdateDto, jwt.getClaim("id"));
    }

    @GetMapping()
    public List<SpectorResponseDto> get(@RequestParam(value = "name", required = false) String name) {
        return spectorService.find(null, name);
    }

    @GetMapping("{id}")
    public SpectorResponseDto getById(@PathVariable(value = "id") Long id) {
        return spectorService.find(id, null).getFirst();
    }

    @DeleteMapping("{id}")
    public AnswerDto delete(
            @PathVariable(value = "id") Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return spectorService.delete(id, jwt.getClaim("id"));
    }

    @PatchMapping("{id}")
    public SpectorResponseDto patch(
            @PathVariable(value = "id") Long id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody SpectorCreateUpdateDto spectorCreateUpdateDto
    ) {
        return spectorService.update(id, jwt.getClaim("id"), spectorCreateUpdateDto);
    }

    @PostMapping("{id}/subscribers")
    public AnswerDto addSubscriber(
            @PathVariable(value = "id") Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return spectorService.addSubscriber(id, jwt.getClaim("id"));
    }

    @GetMapping("{id}/subscribers")
    public Mono<Set<UserInternalResponseDto>> getSubscribers(@PathVariable(value = "id") Long id) {
        return spectorService.getSubscribers(id);
    }

    @DeleteMapping("{id}/subscribers")
    public AnswerDto removeSubscriber(
            @PathVariable(value = "id") Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return spectorService.removeSubscriber(id, jwt.getClaim("id"));
    }

}
