package spectra.ru.community.store.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "message")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Builder.Default
    @Column(columnDefinition = "timestamp without time zone")
    Instant createAt = Instant.now();

    String body;

    Long userId;

    @ManyToOne
    @JoinColumn(name = "parent_message_id")
    MessageEntity parentMessage;

    @ManyToOne
    @JoinColumn(name = "spector_id", nullable = false)
    SpectorEntity spector;

    @Builder.Default
    @OneToMany(mappedBy = "parentMessage", orphanRemoval = true)
    List<MessageEntity> comment = new ArrayList<>();

    @Builder.Default
    @OneToMany
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    List<ReactionEntity> reactionEntity = new ArrayList<>();

}
