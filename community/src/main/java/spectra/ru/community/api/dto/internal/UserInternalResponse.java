package spectra.ru.community.api.dto.internal;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserInternalResponse {

    Set<UserInternalResponseDto> userList;

}
