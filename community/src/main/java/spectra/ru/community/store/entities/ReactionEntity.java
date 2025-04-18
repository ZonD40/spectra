package spectra.ru.community.store.entities;

import spectra.ru.community.enums.ReactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "reaction")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long userId;

    @Builder.Default
    @Column(columnDefinition = "timestamp without time zone", nullable = false)
    Instant createAt = Instant.now();

    @Column(nullable = false)
    ReactionType type;

    @ManyToOne
    @JoinColumn(name = "message_id", nullable = false)
    MessageEntity message;

}
