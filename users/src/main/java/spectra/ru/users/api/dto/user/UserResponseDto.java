package spectra.ru.users.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponseDto {

    @NotNull
    Long id;

    @NotNull
    Instant createAt;

    @NotBlank
    String name;

    @NotBlank
    String email;

}
