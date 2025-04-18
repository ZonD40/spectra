package spectra.ru.community.api.dto.message;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageResponseDto {

    Long id;

    String body;

    Instant createAt;

    Long spectorId;

    Long parentMessageId;

    Long userId;

}
