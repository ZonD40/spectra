package spectra.ru.community.api.factories;

import spectra.ru.community.api.dto.spector.SpectorResponseDto;
import spectra.ru.community.store.entities.SpectorEntity;
import org.springframework.stereotype.Component;

@Component
public class SpectorDtoFactory {

    public SpectorResponseDto makeSpectorDto(SpectorEntity entity) {
        return SpectorResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createAt(entity.getCreateAt())
                .build();
    }
}
