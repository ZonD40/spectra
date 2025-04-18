package spectra.ru.users.api.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnswerDto {

    Boolean success;

    public static AnswerDto makeDefault(Boolean answer) {
        return builder()
                .success(answer)
                .build();
    }
}