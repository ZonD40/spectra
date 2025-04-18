package spectra.ru.community.api.dto.reaction;

import spectra.ru.community.enums.ReactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReactionResponseDto {

    Long id;

    ReactionType type;

    Instant createAt;

    Long messageId;

    Long userId;

}
