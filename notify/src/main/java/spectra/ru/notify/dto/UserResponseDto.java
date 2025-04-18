package spectra.ru.notify.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponseDto {

    @NonNull
    Long id;

    @NonNull
    Instant createAt;

    @NonNull
    String name;

    @NonNull
    String email;

}
