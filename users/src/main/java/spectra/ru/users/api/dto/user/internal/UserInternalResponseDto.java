package spectra.ru.users.api.dto.user.internal;

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
