package spectra.ru.community.api.dto.message;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageUpdateDto {

    @NonNull
    String body;

}
