package spectra.ru.community.api.controller;

import spectra.ru.community.api.dto.AnswerDto;
import spectra.ru.community.api.dto.reaction.ReactionResponseDto;
import spectra.ru.community.service.ReactionService;
import spectra.ru.community.enums.ReactionType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "api/messages/{messageId}/reactions")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReactionController {

    ReactionService reactionService;

    @PostMapping
    public ReactionResponseDto create(
            @PathVariable(value = "messageId") Long messageId,
            @RequestParam(value = "reactionType") ReactionType reactionType,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return reactionService.create(messageId, reactionType, jwt.getClaim("id"));
    }

    @GetMapping
    public List<ReactionResponseDto> get(@PathVariable(value = "messageId") Long messageId) {
        return reactionService.find(messageId);
    }

    @DeleteMapping("{id}")
    public AnswerDto delete(
            @PathVariable(value = "messageId") Long messageId,
            @PathVariable(value = "id") Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return reactionService.delete(messageId, id, jwt.getClaim("id"));
    }

}
