package spectra.ru.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewPostEvent {

    String spectorName;

    String postLink;

    List<Long> subscribersIdList;

}
