package spectra.ru.community.api.dto.spector;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpectorResponseDto {

    Long id;

    String name;

    Instant createAt;

    String description;

}
