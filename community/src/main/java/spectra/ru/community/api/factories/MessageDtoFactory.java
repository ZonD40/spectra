package spectra.ru.community.api.factories;

import spectra.ru.community.api.dto.message.MessageResponseDto;
import spectra.ru.community.store.entities.MessageEntity;
import org.springframework.stereotype.Component;

@Component
public class MessageDtoFactory {

    public MessageResponseDto makeMessageDto(MessageEntity entity) {
        return MessageResponseDto.builder()
                .id(entity.getId())
                .body(entity.getBody())
                .createAt(entity.getCreateAt())
                .spectorId(entity.getSpector().getId())
                .userId(entity.getUserId())
                .parentMessageId(entity.getParentMessage() == null ? null : entity.getParentMessage().getId())
                .build();
    }
}
