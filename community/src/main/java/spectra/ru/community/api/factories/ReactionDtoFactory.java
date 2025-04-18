package spectra.ru.community.api.factories;

import spectra.ru.community.api.dto.reaction.ReactionResponseDto;
import spectra.ru.community.store.entities.ReactionEntity;
import org.springframework.stereotype.Component;

@Component
public class ReactionDtoFactory {

    public ReactionResponseDto makeReactionDto(ReactionEntity entity) {
        return ReactionResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .createAt(entity.getCreateAt())
                .userId(entity.getUserId())
                .messageId(entity.getMessage().getId())
                .build();
    }

}
