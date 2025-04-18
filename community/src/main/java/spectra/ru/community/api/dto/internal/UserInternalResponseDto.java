package spectra.ru.community.api.dto.internal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserInternalResponseDto {

    Long id;

    String name;

}
