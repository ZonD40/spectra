package spectra.ru.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewCommentEvent {

    String spectorName;

    String messageLink;

    String commentLink;

    Long userId;

}
