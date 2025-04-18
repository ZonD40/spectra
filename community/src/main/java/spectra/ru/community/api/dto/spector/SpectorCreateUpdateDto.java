package spectra.ru.community.api.dto.spector;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpectorCreateUpdateDto {

    String name;

    String description;

}
