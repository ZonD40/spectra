package spectra.ru.community.store.repository;

import spectra.ru.community.store.entities.MessageEntity;
import spectra.ru.community.store.entities.ReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.stream.Stream;

public interface ReactionRepository extends JpaRepository<ReactionEntity, Long> {

    Stream<ReactionEntity> streamAllByMessage(MessageEntity message);

}
