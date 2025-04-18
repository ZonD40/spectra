package spectra.ru.community.api.controller;

import spectra.ru.community.api.dto.AnswerDto;
import spectra.ru.community.api.dto.message.MessageCreateDto;
import spectra.ru.community.api.dto.message.MessageResponseDto;
import spectra.ru.community.api.dto.message.MessageUpdateDto;
import spectra.ru.community.service.MessageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "api/messages")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageController {

    MessageService messageService;

    @PostMapping
    public MessageResponseDto create(
            @RequestBody MessageCreateDto messageCreateDto,
            @AuthenticationPrincipal Jwt jwt) {
        return messageService.create(messageCreateDto, jwt.getClaim("id"));
    }

    @GetMapping
    public List<MessageResponseDto> get(
            @RequestParam(value = "spectorId", required = false) Long spectorId,
            @RequestParam(value = "parentMessageId", required = false) Long parentId
    ) {
        return messageService.find(spectorId, parentId);
    }

    @GetMapping("{id}")
    public MessageResponseDto get(@PathVariable(value = "id") Long id) {
        return messageService.findById(id);
    }

    @DeleteMapping("{id}")
    public AnswerDto delete(
            @PathVariable(value = "id") Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return messageService.delete(id, jwt.getClaim("id"));
    }

    @PatchMapping("{id}")
    public MessageResponseDto patch(
            @PathVariable(value = "id") Long id,
            @RequestBody MessageUpdateDto messageUpdateDto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return messageService.update(id, messageUpdateDto, jwt.getClaim("id"));
    }

}
