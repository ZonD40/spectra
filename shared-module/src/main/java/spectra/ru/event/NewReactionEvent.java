package spectra.ru.event;

import lombok.*;
        import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewReactionEvent {

    String messageLink;

    String reactionType;

    Long userId;

    Long reactionUserId;

}
