package spectra.ru.community.store.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "spector")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpectorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    @Builder.Default
    @Column(columnDefinition = "timestamp without time zone", nullable = false)
    Instant createAt = Instant.now();

    String description;

    @Builder.Default
    @OneToMany
    @JoinColumn(name = "spector_id", referencedColumnName = "id")
    List<MessageEntity> messageEntity = new ArrayList<>();

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "spector_id", referencedColumnName = "id")
    List<SpectorSubscriberEntity> subscriberList = new ArrayList<>();

}
